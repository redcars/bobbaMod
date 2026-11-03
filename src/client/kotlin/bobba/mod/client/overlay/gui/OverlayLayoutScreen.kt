package bobba.mod.client.overlay.gui

import bobba.mod.client.media.LoadedImage
import bobba.mod.client.media.MediaTextures
import bobba.mod.client.overlay.ImageOverlay
import bobba.mod.client.overlay.ImageOverlays
import bobba.mod.client.overlay.OverlayHud
import bobba.mod.client.overlay.OverlayRect
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.roundToInt

/**
 * Full-screen editor that shows the overlays where they will appear in game. Drag an overlay to
 * move it, drag its corner handle or scroll over it to resize, arrow keys nudge the selected one.
 */
class OverlayLayoutScreen(private val parent: Screen?) : Screen(Component.literal("Overlay Layout")) {

    private companion object {
        const val HANDLE_SIZE = 7
        val OUTLINE = 0xFF808080.toInt()
        val SELECTED = 0xFFFFFFFF.toInt()
        val TEXT = 0xFFFFFFFF.toInt()
        val HINT_BG = 0xA0000000.toInt()
    }

    private enum class Drag { NONE, MOVE, RESIZE }

    private var selected: ImageOverlay? = null
    private var drag = Drag.NONE
    private var grabX = 0.0
    private var grabY = 0.0

    /** Enabled overlays whose image has loaded, with their current bounds, in draw order. */
    private fun visible(): List<Triple<ImageOverlay, LoadedImage, OverlayRect>> =
        ImageOverlays.overlays.filter { it.enabled }.mapNotNull { overlay ->
            val image = MediaTextures.get(overlay.file) ?: return@mapNotNull null
            Triple(overlay, image, OverlayHud.rect(overlay, image, width, height))
        }

    private fun handleRect(rect: OverlayRect) =
        OverlayRect(rect.right - HANDLE_SIZE + 2, rect.bottom - HANDLE_SIZE + 2, HANDLE_SIZE, HANDLE_SIZE)

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // In game, keep the world visible and sharp so overlays can be placed against it.
        if (minecraft.level == null) super.extractBackground(graphics, mouseX, mouseY, delta)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        for ((overlay, image, rect) in visible()) {
            OverlayHud.draw(graphics, overlay, image, rect)
            val color = if (overlay === selected) SELECTED else OUTLINE
            graphics.outline(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2, color)
            if (overlay === selected) {
                val handle = handleRect(rect)
                graphics.fill(handle.x, handle.y, handle.right, handle.bottom, SELECTED)
            }
        }

        val hint = if (ImageOverlays.overlays.none { it.enabled }) {
            "No overlays are turned on. Press Esc to go back."
        } else {
            "Drag to move · drag the corner or scroll to resize · arrows nudge (Shift = 10) · Esc when done"
        }
        drawLabel(graphics, hint, 6)

        selected?.let { overlay ->
            val label = "${overlay.file} · ${(overlay.width * 100).roundToInt()}% of screen width · " +
                "opacity ${(overlay.opacity * 100).roundToInt()}%"
            drawLabel(graphics, label, height - 16)
        }
    }

    private fun drawLabel(graphics: GuiGraphicsExtractor, text: String, y: Int) {
        val w = font.width(text)
        graphics.fill(width / 2 - w / 2 - 4, y - 3, width / 2 + w / 2 + 4, y + 11, HINT_BG)
        graphics.centeredText(font, Component.literal(text), width / 2, y, TEXT)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick)
        val shown = visible()

        // The selected overlay's handle wins, even where it overlaps another overlay.
        shown.firstOrNull { it.first === selected }?.let { (_, _, rect) ->
            if (handleRect(rect).contains(event.x(), event.y())) {
                drag = Drag.RESIZE
                return true
            }
        }

        // Topmost (last drawn) overlay under the cursor.
        val hit = shown.lastOrNull { it.third.contains(event.x(), event.y()) }
        selected = hit?.first
        if (hit == null) {
            drag = Drag.NONE
            return super.mouseClicked(event, doubleClick)
        }
        drag = Drag.MOVE
        grabX = event.x() - hit.third.x
        grabY = event.y() - hit.third.y
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val overlay = selected ?: return super.mouseDragged(event, dragX, dragY)
        val image = MediaTextures.get(overlay.file) ?: return super.mouseDragged(event, dragX, dragY)
        when (drag) {
            Drag.MOVE -> moveTo(overlay, image, event.x() - grabX, event.y() - grabY)
            Drag.RESIZE -> {
                val rect = OverlayHud.rect(overlay, image, width, height)
                setWidth(overlay, event.x() - rect.x)
            }
            Drag.NONE -> return super.mouseDragged(event, dragX, dragY)
        }
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (drag != Drag.NONE) {
            drag = Drag.NONE
            ImageOverlays.save()
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val hit = visible().lastOrNull { it.third.contains(mouseX, mouseY) }
            ?: return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        val (overlay, image, rect) = hit
        selected = overlay
        // Scale around the centre so the overlay doesn't drift while resizing.
        val factor = if (scrollY > 0) 1.1 else 1 / 1.1
        val centerX = rect.x + rect.width / 2.0
        val centerY = rect.y + rect.height / 2.0
        setWidth(overlay, rect.width * factor)
        val resized = OverlayHud.rect(overlay, image, width, height)
        moveTo(overlay, image, centerX - resized.width / 2.0, centerY - resized.height / 2.0)
        ImageOverlays.save()
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val overlay = selected
        val image = overlay?.let { MediaTextures.get(it.file) }
        if (overlay != null && image != null) {
            val step = if (event.hasShiftDown()) 10 else 1
            val (dx, dy) = when {
                event.isLeft -> -step to 0
                event.isRight -> step to 0
                event.isUp -> 0 to -step
                event.isDown -> 0 to step
                else -> 0 to 0
            }
            if (dx != 0 || dy != 0) {
                val rect = OverlayHud.rect(overlay, image, width, height)
                moveTo(overlay, image, (rect.x + dx).toDouble(), (rect.y + dy).toDouble())
                ImageOverlays.save()
                return true
            }
        }
        return super.keyPressed(event)
    }

    /** Moves the top-left corner to ([px], [py]), keeping at least a sliver of the overlay on screen. */
    private fun moveTo(overlay: ImageOverlay, image: LoadedImage, px: Double, py: Double) {
        val rect = OverlayHud.rect(overlay, image, width, height)
        val keep = OverlayHud.MIN_SIZE
        overlay.x = px.coerceIn((keep - rect.width).toDouble(), (width - keep).toDouble()) / width
        overlay.y = py.coerceIn((keep - rect.height).toDouble(), (height - keep).toDouble()) / height
    }

    private fun setWidth(overlay: ImageOverlay, pixels: Double) {
        overlay.width = pixels.coerceIn(OverlayHud.MIN_SIZE.toDouble(), width * 2.0) / width
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        ImageOverlays.save()
        minecraft.gui.setScreen(parent)
    }
}
