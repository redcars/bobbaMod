package bobba.mod.client.overlay

import java.util.UUID

/**
 * A picture or GIF drawn on the HUD. Position and size are fractions of the screen so the
 * placement survives window resizes and GUI scale changes; height follows the image's aspect ratio.
 */
data class ImageOverlay(
    val id: String = UUID.randomUUID().toString(),
    /** Stored name in the media library. */
    val file: String,
    var enabled: Boolean = true,
    /** Left edge, as a fraction of the screen width. */
    var x: Double = 0.05,
    /** Top edge, as a fraction of the screen height. */
    var y: Double = 0.05,
    /** Width, as a fraction of the screen width. */
    var width: Double = 0.2,
    var opacity: Double = 1.0,
)
