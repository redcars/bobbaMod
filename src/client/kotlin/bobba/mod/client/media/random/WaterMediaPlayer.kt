package bobba.mod.client.media.random

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import org.watermedia.WaterMedia
import org.watermedia.api.codecs.CodecsAPI
import org.watermedia.api.player.AWTEngine
import org.watermedia.api.player.MRL
import org.watermedia.api.player.MediaPlayer
import org.watermedia.api.player.PlayerAPI
import org.slf4j.LoggerFactory
import java.awt.image.DataBufferInt
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * [ClipPlayer] backed by WATERMeDIA (FFmpeg from its Binaries mod). Only constructed when the mod
 * is installed, so its classes never load otherwise. Render thread only.
 */
class WaterMediaPlayer : ClipPlayer {
    private companion object {
        // Frames are copied on the CPU every frame; bigger than this costs FPS for no visible gain.
        const val MAX_VIDEO_WIDTH = 1280
        const val MAX_VIDEO_HEIGHT = 720
    }

    private val logger = LoggerFactory.getLogger("bobbamod/random-media")

    private var mrl: MRL? = null
    private var player: MediaPlayer? = null
    private var engine: AWTEngine? = null
    private var video: VideoTexture? = null
    private var volume = 1.0
    private var size = 0.5

    // Set on WATERMeDIA's decode thread when a new frame is in the engine's image.
    private val frameReady = AtomicBoolean(false)

    override val busy get() = mrl != null || player != null
    override var lastError: String? = null
        private set

    override fun play(file: Path, volume: Double, size: Double) {
        stop()
        lastError = when {
            !WaterMedia.loaded() -> "WATERMeDIA is still starting up"
            !CodecsAPI.ffmpegLoaded() -> "WATERMeDIA can't find FFmpeg; install WATERMeDIA: Binaries"
            else -> null
        }
        if (lastError != null) return
        this.volume = volume
        this.size = size
        // Loads in the background; tick() builds the player once it's ready.
        mrl = PlayerAPI.mrl(file.toAbsolutePath().toString())
    }

    override fun tick() {
        val pending = mrl
        if (pending != null && player == null) {
            if (pending.status().failed()) return fail(pending.exception()?.message ?: "can't open the file")
            val created = PlayerAPI.player(
                pending,
                { PlayerAPI.awtEngine { frameReady.set(true) }.also { engine = it } },
                { PlayerAPI.alEngine() },
            ) ?: return // still loading
            created.scale(MAX_VIDEO_WIDTH, MAX_VIDEO_HEIGHT)
            created.volume((volume * 100).roundToInt())
            created.start()
            player = created
        }
        val current = player ?: return
        when (current.status()) {
            MediaPlayer.Status.ENDED -> stop()
            MediaPlayer.Status.ERROR -> fail(current.exception()?.message ?: "playback failed")
            else -> {}
        }
    }

    override fun render(graphics: GuiGraphicsExtractor) {
        if (player == null) return
        val image = engine?.image()
        if (image != null && frameReady.getAndSet(false)) {
            val texture = video ?: VideoTexture(Identifier.fromNamespaceAndPath("bobbamod", "random_media/video")).also { video = it }
            texture.upload((image.raster.dataBuffer as DataBufferInt).data, image.width, image.height)
        }
        val texture = video ?: return
        if (texture.width == 0) return

        // Centred, [size] of the screen width, shrunk further if it would be taller than the screen.
        val screenW = graphics.guiWidth()
        val screenH = graphics.guiHeight()
        var w = (screenW * size).roundToInt()
        var h = (w.toDouble() * texture.height / texture.width).roundToInt()
        if (h > screenH) {
            h = screenH
            w = (h.toDouble() * texture.width / texture.height).roundToInt()
        }
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, texture.id,
            (screenW - w) / 2, (screenH - h) / 2, 0f, 0f,
            w, h, texture.width, texture.height, texture.width, texture.height,
            -1,
        )
    }

    override fun stop() {
        player?.let {
            it.stop()
            it.release()
        }
        player = null
        mrl = null
        engine = null
        frameReady.set(false)
        video?.close()
        video = null
    }

    private fun fail(reason: String) {
        logger.warn("Random media clip failed: {}", reason)
        stop()
        lastError = reason
    }
}
