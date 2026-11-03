package bobba.mod.client.config.editor

import bobba.mod.client.media.MediaDropTarget
import bobba.mod.client.media.MediaFilePicker
import bobba.mod.client.media.MediaTextures
import bobba.mod.client.overlay.ImageOverlay
import bobba.mod.client.overlay.ImageOverlays
import bobba.mod.client.overlay.gui.OverlayLayoutScreen
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.minecraft.client.Minecraft
import java.nio.file.Path
import kotlin.math.roundToInt

/** The whole overlay list inside the Overlays tab: add, edit layout, and per-overlay controls. */
class GuiOptionEditorOverlayList(option: ProcessedOption) : GuiOptionEditor(option) {

    private companion object {
        const val PAD = 8
        const val BUTTON_WIDTH = 84
        const val BUTTON_HEIGHT = 18
        const val LIST_TOP = 48
        const val ROW_HEIGHT = 24
        const val THUMB = 20
        const val TOGGLE_WIDTH = 34
        const val SLIDER_WIDTH = 90
        const val REMOVE_WIDTH = 18
        const val CONTROL_HEIGHT = 16
        const val GAP = 6
    }

    private class Row(val overlay: ImageOverlay, val thumb: Box, val nameX: Int, val nameWidth: Int, val toggle: Box, val slider: Box, val remove: Box)

    private var status: String? = null
    private var dragging: ImageOverlay? = null

    private fun addButton(x: Int, y: Int, width: Int) = Box(x + width - PAD - 2 * BUTTON_WIDTH - GAP, y + PAD, BUTTON_WIDTH, BUTTON_HEIGHT)
    private fun layoutButton(x: Int, y: Int, width: Int) = Box(x + width - PAD - BUTTON_WIDTH, y + PAD, BUTTON_WIDTH, BUTTON_HEIGHT)

    private fun rows(x: Int, y: Int, width: Int): List<Row> =
        ImageOverlays.overlays.mapIndexed { i, overlay ->
            val rowY = y + LIST_TOP + i * ROW_HEIGHT
            val controlY = rowY + (THUMB - CONTROL_HEIGHT) / 2
            val remove = Box(x + width - PAD - REMOVE_WIDTH, controlY, REMOVE_WIDTH, CONTROL_HEIGHT)
            val slider = Box(remove.x - GAP - SLIDER_WIDTH, controlY, SLIDER_WIDTH, CONTROL_HEIGHT)
            val toggle = Box(slider.x - GAP - TOGGLE_WIDTH, controlY, TOGGLE_WIDTH, CONTROL_HEIGHT)
            val nameX = x + PAD + THUMB + GAP
            Row(overlay, Box(x + PAD, rowY, THUMB, THUMB), nameX, toggle.x - GAP - nameX, toggle, slider, remove)
        }

    override fun getHeight(): Int {
        val rows = ImageOverlays.overlays.size.coerceAtLeast(1)
        return LIST_TOP + rows * ROW_HEIGHT + (if (status != null) 14 else 0) + PAD
    }

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        MediaDropTarget.offer(::import)
        val fr = context.minecraft.defaultFontRenderer
        context.drawDarkRect(x, y, width, height, true)

        EditorDraw.text(context, fr, "Image overlays", x + PAD, y + PAD + 5, EditorDraw.WHITE)
        EditorDraw.button(context, fr, addButton(x, y, width), "Add image...")
        EditorDraw.button(context, fr, layoutButton(x, y, width), "Edit layout")
        EditorDraw.text(
            context, fr, "Or drag PNG, JPG, GIF or BMP files onto this window.",
            x + PAD, y + PAD + BUTTON_HEIGHT + 6, EditorDraw.GRAY, width - 2 * PAD,
        )

        val rows = rows(x, y, width)
        if (rows.isEmpty()) {
            EditorDraw.text(context, fr, "No overlays yet.", x + PAD, y + LIST_TOP + 6, EditorDraw.GRAY)
        }
        for (row in rows) {
            val overlay = row.overlay
            MediaTextures.get(overlay.file)?.let { EditorDraw.image(context, it, EditorDraw.fit(it, row.thumb)) }
            val error = MediaTextures.error(overlay.file)
            val label = if (error != null) "${overlay.file} — can't load: $error" else overlay.file
            val color = if (error != null) EditorDraw.RED else if (overlay.enabled) EditorDraw.WHITE else EditorDraw.GRAY
            EditorDraw.text(context, fr, label, row.nameX, row.thumb.y + 6, color, row.nameWidth)
            EditorDraw.button(context, fr, row.toggle, if (overlay.enabled) "On" else "Off")
            EditorDraw.slider(context, fr, row.slider, overlay.opacity, "Opacity ${(overlay.opacity * 100).roundToInt()}%")
            EditorDraw.button(context, fr, row.remove, "X")
        }

        status?.let {
            EditorDraw.text(context, fr, it, x + PAD, y + height - PAD - 10, EditorDraw.GRAY, width - 2 * PAD)
        }
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, mouseEvent: MouseEvent): Boolean {
        dragging?.let { overlay ->
            when (mouseEvent) {
                is MouseEvent.Move -> {
                    rows(x, y, width).firstOrNull { it.overlay === overlay }?.let { setOpacity(overlay, it.slider, mouseX) }
                    return true
                }
                is MouseEvent.Click -> if (!mouseEvent.mouseState) {
                    dragging = null
                    ImageOverlays.save()
                    return true
                }
                else -> {}
            }
        }

        if (mouseEvent !is MouseEvent.Click || !mouseEvent.mouseState || mouseEvent.mouseButton != 0) return false

        if (addButton(x, y, width).contains(mouseX, mouseY)) {
            MediaFilePicker.pickImages(::import)
            return true
        }
        if (layoutButton(x, y, width).contains(mouseX, mouseY)) {
            val mc = Minecraft.getInstance()
            mc.gui.setScreen(OverlayLayoutScreen(mc.gui.screen()))
            return true
        }
        for (row in rows(x, y, width)) {
            when {
                row.toggle.contains(mouseX, mouseY) -> {
                    row.overlay.enabled = !row.overlay.enabled
                    ImageOverlays.save()
                }
                row.slider.contains(mouseX, mouseY) -> {
                    dragging = row.overlay
                    setOpacity(row.overlay, row.slider, mouseX)
                }
                row.remove.contains(mouseX, mouseY) -> ImageOverlays.remove(row.overlay)
                else -> continue
            }
            return true
        }
        return false
    }

    private fun setOpacity(overlay: ImageOverlay, slider: Box, mouseX: Int) {
        overlay.opacity = ((mouseX - slider.x).toDouble() / slider.width).coerceIn(0.0, 1.0)
    }

    private fun import(paths: List<Path>) {
        status = "Importing ${paths.size} file(s)..."
        ImageOverlays.importImages(paths) { added, errors ->
            status = when {
                errors.isEmpty() -> "Added $added overlay(s). Use Edit layout to place them."
                else -> "Added $added. Skipped: ${errors.joinToString("; ")}"
            }
        }
    }

    override fun fulfillsSearch(word: String): Boolean =
        super.fulfillsSearch(word) || word in "overlay" || word in "image" || word in "gif"
}
