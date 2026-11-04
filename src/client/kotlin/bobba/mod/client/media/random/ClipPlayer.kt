package bobba.mod.client.media.random

import net.minecraft.client.gui.GuiGraphicsExtractor
import java.nio.file.Path

/**
 * Plays one clip at a time. The only implementation needs WATERMeDIA, which players install
 * themselves, so nothing outside [WaterMediaPlayer] may touch its classes. Render thread only.
 */
interface ClipPlayer {
    /** True while a clip is loading or playing. */
    val busy: Boolean

    /** Why the last clip didn't play, or null. */
    val lastError: String?

    /** Stops whatever is playing and starts [file]. [size] is the video width as a fraction of the screen. */
    fun play(file: Path, volume: Double, size: Double)

    fun stop()

    /** Advances loading and notices the end of playback; call every client tick. */
    fun tick()

    /** Draws the current video frame, if any. */
    fun render(graphics: GuiGraphicsExtractor)
}
