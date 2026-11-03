package bobba.mod.client.media

import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.math.sqrt

/** An image or GIF uploaded to the GPU, one texture per frame. */
class LoadedImage(val width: Int, val height: Int, val frames: List<Identifier>, private val delaysMs: IntArray) {
    private val totalMs = delaysMs.sum()

    fun frameAt(timeMs: Long): Identifier {
        if (frames.size == 1) return frames[0]
        var t = (timeMs % totalMs).toInt()
        for (i in frames.indices) {
            t -= delaysMs[i]
            if (t < 0) return frames[i]
        }
        return frames.last()
    }
}

/**
 * Decodes media files off-thread and caches their textures by stored file name. All public
 * methods are render-thread only.
 */
object MediaTextures {
    // VRAM budget per file (4 bytes per pixel, so 128 MB); bigger files are scaled down to fit.
    private const val MAX_TOTAL_PIXELS = 32L * 1024 * 1024
    private const val MAX_DIMENSION = 4096

    // stb decodes every GIF frame into one buffer before we can scale anything down.
    private const val MAX_DECODE_BYTES = 1024L * 1024 * 1024

    // Browsers treat GIF delays under 20 ms as 100 ms; many GIFs rely on that.
    private const val MIN_GIF_DELAY_MS = 20
    private const val DEFAULT_GIF_DELAY_MS = 100

    private val logger = LoggerFactory.getLogger("bobbamod/media")
    private val decoder = Executors.newSingleThreadExecutor { r ->
        Thread(r, "bobbamod-media-decoder").apply { isDaemon = true }
    }

    private sealed interface State
    private class Loading : State
    private class Ready(val image: LoadedImage) : State
    private class Failed(val reason: String) : State

    private val cache = HashMap<String, State>()
    private var nextTextureId = 0

    /** The image if it is ready; otherwise starts loading it and returns null. */
    fun get(name: String): LoadedImage? {
        val state = cache.getOrPut(name) { startLoad(name) }
        return (state as? Ready)?.image
    }

    /** Why [name] could not be loaded, or null if it loaded or is still loading. */
    fun error(name: String): String? = (cache[name] as? Failed)?.reason

    fun release(name: String) {
        val state = cache.remove(name)
        if (state is Ready) {
            val textures = Minecraft.getInstance().textureManager
            state.image.frames.forEach { textures.release(it) }
        }
    }

    private fun startLoad(name: String): State {
        val loading = Loading()
        CompletableFuture.supplyAsync({ decode(name) }, decoder).whenComplete { decoded, err ->
            Minecraft.getInstance().execute {
                // Released or reloaded while decoding: drop this result.
                if (cache[name] !== loading) {
                    decoded?.frames?.forEach { it.close() }
                    return@execute
                }
                cache[name] = if (decoded != null) {
                    Ready(upload(decoded))
                } else {
                    val reason = (err?.cause ?: err)?.message ?: "unknown error"
                    logger.warn("Failed to load media file {}: {}", name, reason)
                    Failed(reason)
                }
            }
        }
        return loading
    }

    private class Decoded(val width: Int, val height: Int, val frames: List<NativeImage>, val delaysMs: IntArray)

    private fun decode(name: String): Decoded {
        val bytes = Files.readAllBytes(MediaLibrary.resolve(name))
        val buffer = MemoryUtil.memAlloc(bytes.size).put(bytes).flip()
        try {
            MemoryStack.stackPush().use { stack ->
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)
                val channels = stack.mallocInt(1)
                if (isGif(bytes)) {
                    checkDecodeSize(bytes)
                    val delays = stack.mallocPointer(1)
                    val layers = stack.mallocInt(1)
                    val pixels = STBImage.stbi_load_gif_from_memory(buffer, delays, w, h, layers, channels, 4)
                        ?: throw IOException(STBImage.stbi_failure_reason() ?: "not a valid GIF")
                    try {
                        val count = layers.get(0)
                        val delaysMs = IntArray(count) {
                            val d = MemoryUtil.memGetInt(delays.get(0) + it * 4L)
                            if (d < MIN_GIF_DELAY_MS) DEFAULT_GIF_DELAY_MS else d
                        }
                        val (tw, th) = fitSize(w.get(0), h.get(0), count)
                        val frameBytes = w.get(0).toLong() * h.get(0) * 4
                        val frames = (0 until count).map {
                            toNativeImage(MemoryUtil.memAddress(pixels) + it * frameBytes, w.get(0), h.get(0), tw, th)
                        }
                        return Decoded(tw, th, frames, delaysMs)
                    } finally {
                        STBImage.stbi_image_free(pixels)
                        if (delays.get(0) != 0L) STBImage.nstbi_image_free(delays.get(0))
                    }
                }
                val pixels = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4)
                    ?: throw IOException(STBImage.stbi_failure_reason() ?: "not a supported image")
                try {
                    val (tw, th) = fitSize(w.get(0), h.get(0), 1)
                    val frame = toNativeImage(MemoryUtil.memAddress(pixels), w.get(0), h.get(0), tw, th)
                    return Decoded(tw, th, listOf(frame), intArrayOf(DEFAULT_GIF_DELAY_MS))
                } finally {
                    STBImage.stbi_image_free(pixels)
                }
            }
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    private fun isGif(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == '8'.code.toByte()

    /** Refuses GIFs whose fully decoded frames would not fit in [MAX_DECODE_BYTES]. */
    private fun checkDecodeSize(bytes: ByteArray) {
        if (bytes.size < 10) return
        val width = (bytes[6].toInt() and 0xFF) or ((bytes[7].toInt() and 0xFF) shl 8)
        val height = (bytes[8].toInt() and 0xFF) or ((bytes[9].toInt() and 0xFF) shl 8)
        val frames = countGifFrames(bytes)
        if (width.toLong() * height * frames * 4 > MAX_DECODE_BYTES) {
            throw IOException("too big to load ($frames frames of ${width}x$height)")
        }
    }

    /** Counts image descriptors by walking the GIF block structure, without decoding pixels. */
    private fun countGifFrames(b: ByteArray): Int {
        fun u8(i: Int) = b[i].toInt() and 0xFF
        var frames = 0
        try {
            var i = 13
            if (u8(10) and 0x80 != 0) i += 3 * (1 shl ((u8(10) and 7) + 1))
            fun skipSubBlocks() {
                while (true) {
                    val n = u8(i)
                    i += 1 + n
                    if (n == 0) return
                }
            }
            while (i < b.size) {
                when (u8(i)) {
                    0x2C -> {
                        frames++
                        val packed = u8(i + 9)
                        i += 10
                        if (packed and 0x80 != 0) i += 3 * (1 shl ((packed and 7) + 1))
                        i++ // LZW minimum code size
                        skipSubBlocks()
                    }
                    0x21 -> {
                        i += 2
                        skipSubBlocks()
                    }
                    else -> return frames
                }
            }
        } catch (_: IndexOutOfBoundsException) {
            // Truncated file: stb decodes the frames that are complete, so count those.
        }
        return frames
    }

    /** Size to store frames at: at most [MAX_DIMENSION] per side and [MAX_TOTAL_PIXELS] over all frames. */
    private fun fitSize(width: Int, height: Int, frames: Int): Pair<Int, Int> {
        val scale = minOf(
            1.0,
            MAX_DIMENSION.toDouble() / maxOf(width, height),
            sqrt(MAX_TOTAL_PIXELS.toDouble() / (width.toLong() * height * frames)),
        )
        if (scale >= 1.0) return width to height
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }

    // stb's RGBA byte order matches NativeImage's RGBA layout, so a straight copy works.
    private fun toNativeImage(src: Long, width: Int, height: Int, targetWidth: Int, targetHeight: Int): NativeImage {
        val image = NativeImage(NativeImage.Format.RGBA, width, height, false)
        MemoryUtil.memCopy(src, image.pointer, width.toLong() * height * 4)
        if (targetWidth == width && targetHeight == height) return image
        image.use {
            val scaled = NativeImage(NativeImage.Format.RGBA, targetWidth, targetHeight, false)
            it.resizeSubRectTo(0, 0, width, height, scaled)
            return scaled
        }
    }

    private fun upload(decoded: Decoded): LoadedImage {
        val textures = Minecraft.getInstance().textureManager
        val base = "media/${nextTextureId++}"
        val ids = decoded.frames.mapIndexed { i, frame ->
            val id = Identifier.fromNamespaceAndPath("bobbamod", "$base/$i")
            frame.use { textures.register(id, FrameTexture(id.toString(), it)) }
            id
        }
        return LoadedImage(decoded.width, decoded.height, ids, decoded.delaysMs)
    }

    /**
     * GPU-only texture: unlike DynamicTexture it does not keep the pixels in RAM after upload.
     * Linear filtering, since overlays are almost always drawn scaled and nearest looks jagged.
     */
    private class FrameTexture(label: String, image: NativeImage) : AbstractTexture() {
        init {
            val device = RenderSystem.getDevice()
            val gpuTexture = device.createTexture(
                label,
                GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM,
                image.width, image.height, 1, 1,
            )
            texture = gpuTexture
            textureView = device.createTextureView(gpuTexture)
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            device.createCommandEncoder().writeToTexture(gpuTexture, image)
        }
    }
}
