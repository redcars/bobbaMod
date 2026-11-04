package bobba.mod.client.media.random

import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import org.lwjgl.system.MemoryUtil

/**
 * A texture that video frames are copied into. Frames arrive as CPU pixels and go up through
 * Minecraft's own GPU layer, so this works on both the OpenGL and Vulkan backends.
 * Render thread only.
 */
class VideoTexture(val id: Identifier) : AutoCloseable {
    private class Holder : AbstractTexture() {
        fun set(texture: GpuTexture?) {
            textureView?.close()
            this.texture?.close()
            this.texture = texture
            textureView = texture?.let { RenderSystem.getDevice().createTextureView(it) }
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
        }

        override fun close() = set(null)
    }

    private val holder = Holder()
    private var scratch: NativeImage? = null

    var width = 0
        private set
    var height = 0
        private set

    init {
        Minecraft.getInstance().textureManager.register(id, holder)
    }

    /** Uploads one frame of `0xAARRGGBB` pixels, row by row from the top. */
    fun upload(argb: IntArray, width: Int, height: Int) {
        if (argb.size < width * height) return
        if (width != this.width || height != this.height || scratch == null) resize(width, height)
        val image = scratch ?: return
        val texture = holder.getTexture()

        // NativeImage holds R, G, B, A bytes, i.e. little-endian 0xAABBGGRR ints: swap red and blue.
        val ptr = image.pointer
        for (i in 0 until width * height) {
            val p = argb[i]
            MemoryUtil.memPutInt(ptr + i * 4L, (p and 0xFF00FF00.toInt()) or ((p shr 16) and 0xFF) or ((p and 0xFF) shl 16))
        }
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, image)
    }

    private fun resize(width: Int, height: Int) {
        scratch?.close()
        scratch = NativeImage(NativeImage.Format.RGBA, width, height, false)
        holder.set(
            RenderSystem.getDevice().createTexture(
                id.toString(),
                GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM,
                width, height, 1, 1,
            )
        )
        this.width = width
        this.height = height
    }

    override fun close() {
        Minecraft.getInstance().textureManager.release(id)
        scratch?.close()
        scratch = null
        width = 0
        height = 0
    }
}
