package bobba.mod.client.config.editor

import bobba.mod.client.media.LoadedImage
import io.github.notenoughupdates.moulconfig.GuiTextures
import io.github.notenoughupdates.moulconfig.common.IFontRenderer
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.common.TextureFilter
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import kotlin.math.roundToInt

/** A clickable area inside a custom config editor, in GUI pixels. */
data class Box(val x: Int, val y: Int, val width: Int, val height: Int) {
    val right get() = x + width
    val bottom get() = y + height

    fun contains(px: Int, py: Int) = px >= x && px < right && py >= y && py < bottom
}

/** Drawing helpers that match MoulConfig's own widgets. */
object EditorDraw {
    val WHITE = 0xFFFFFFFF.toInt()
    val GRAY = 0xFFAAAAAA.toInt()
    val RED = 0xFFFF6666.toInt()
    private val BUTTON_TEXT = 0xFF303030.toInt()
    private val BUTTON_DISABLED_TEXT = 0xFF808080.toInt()
    private val SLIDER_BG = 0xFF202020.toInt()
    private val SLIDER_FILL = 0xFF4A7AB0.toInt()
    private val OUTLINE = 0xFF606060.toInt()

    fun text(context: RenderContext, fr: IFontRenderer, text: String, x: Int, y: Int, color: Int, maxWidth: Int = Int.MAX_VALUE) {
        val fitted = if (fr.getStringWidth(text) > maxWidth) fr.trimStringToWidth(text, maxWidth) else text
        context.drawString(fr, StructuredText.of(fitted), x, y, color, true)
    }

    fun button(context: RenderContext, fr: IFontRenderer, box: Box, label: String, enabled: Boolean = true) {
        context.drawTexturedRect(GuiTextures.BUTTON, box.x.toFloat(), box.y.toFloat(), box.width.toFloat(), box.height.toFloat())
        context.drawStringCenteredScaledMaxWidth(
            StructuredText.of(label), fr,
            box.x + box.width / 2f, box.y + box.height / 2f,
            false, box.width - 6,
            if (enabled) BUTTON_TEXT else BUTTON_DISABLED_TEXT,
        )
    }

    /** A 0..1 slider: dark track, filled part, centred label. */
    fun slider(context: RenderContext, fr: IFontRenderer, box: Box, value: Double, label: String) {
        context.drawColoredRect(box.x.toFloat(), box.y.toFloat(), box.right.toFloat(), box.bottom.toFloat(), SLIDER_BG)
        val fill = box.x + (box.width * value.coerceIn(0.0, 1.0)).roundToInt()
        context.drawColoredRect(box.x.toFloat(), box.y.toFloat(), fill.toFloat(), box.bottom.toFloat(), SLIDER_FILL)
        context.drawStringCenteredScaledMaxWidth(
            StructuredText.of(label), fr,
            box.x + box.width / 2f, box.y + box.height / 2f,
            true, box.width - 4, WHITE,
        )
    }

    fun outline(context: RenderContext, box: Box) {
        val x = box.x.toFloat() - 1
        val y = box.y.toFloat() - 1
        val r = box.right.toFloat() + 1
        val b = box.bottom.toFloat() + 1
        context.drawColoredRect(x, y, r, y + 1, OUTLINE)
        context.drawColoredRect(x, b - 1, r, b, OUTLINE)
        context.drawColoredRect(x, y, x + 1, b, OUTLINE)
        context.drawColoredRect(r - 1, y, r, b, OUTLINE)
    }

    /** Draws the current frame of [image] into [box], sampling the normalised [uv] rectangle. */
    fun image(
        context: RenderContext,
        image: LoadedImage,
        box: Box,
        uv: FloatArray = floatArrayOf(0f, 0f, 1f, 1f),
        opacity: Double = 1.0,
    ) {
        val frame = image.frameAt(System.currentTimeMillis())
        val alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
        context.drawTexturedTintedRect(
            MyResourceLocation(frame.namespace, frame.path),
            box.x.toFloat(), box.y.toFloat(), box.width.toFloat(), box.height.toFloat(),
            uv[0], uv[1], uv[2], uv[3],
            (alpha shl 24) or 0xFFFFFF,
            TextureFilter.LINEAR,
        )
    }

    /** [box] shrunk to [image]'s shape and centred, for thumbnails. */
    fun fit(image: LoadedImage, box: Box): Box {
        val scale = minOf(box.width.toDouble() / image.width, box.height.toDouble() / image.height)
        val w = (image.width * scale).roundToInt().coerceAtLeast(1)
        val h = (image.height * scale).roundToInt().coerceAtLeast(1)
        return Box(box.x + (box.width - w) / 2, box.y + (box.height - h) / 2, w, h)
    }
}
