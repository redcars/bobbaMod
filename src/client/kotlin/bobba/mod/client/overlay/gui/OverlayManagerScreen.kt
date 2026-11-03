package bobba.mod.client.overlay.gui

import bobba.mod.client.gui.BobbaScreen
import bobba.mod.client.media.MediaFilePicker
import bobba.mod.client.media.MediaTextures
import bobba.mod.client.overlay.ImageOverlay
import bobba.mod.client.overlay.ImageOverlays
import bobba.mod.client.overlay.OverlayHud
import bobba.mod.client.overlay.OverlayRect
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.nio.file.Path
import kotlin.math.roundToInt

/** Lists image overlays: import, toggle, opacity, remove, and the way into the layout editor. */
class OverlayManagerScreen(parent: Screen?) :
    BobbaScreen(Component.literal("Image Overlays"), parent) {

    companion object {
        private const val MAX_VISIBLE = 7
        private const val ROW_HEIGHT = 24
        private const val THUMB_SIZE = 20
        private const val NAME_WIDTH = 170
        private const val TOGGLE_WIDTH = 36
        private const val SLIDER_WIDTH = 110
        private const val REMOVE_WIDTH = 20
        private const val GAP = 6
        private const val ROW_WIDTH = THUMB_SIZE + GAP + NAME_WIDTH + GAP + TOGGLE_WIDTH + GAP + SLIDER_WIDTH + GAP + REMOVE_WIDTH

        private val GRAY = 0xFFAAAAAA.toInt()
        private val WHITE = 0xFFFFFFFF.toInt()
        private val RED = 0xFFFF6666.toInt()

        fun open() {
            val mc = Minecraft.getInstance()
            mc.execute { mc.gui.setScreen(OverlayManagerScreen(mc.gui.screen())) }
        }
    }

    override val panelWidth: Int = 440

    private var scroll = 0
    private var status: Component? = null

    private val rowX get() = width / 2 - ROW_WIDTH / 2
    private val listTop get() = panelContentTop + 44

    override fun init() {
        val centerX = width / 2
        addRenderableWidget(
            Button.builder(Component.literal("Add image...")) { MediaFilePicker.pickImages(::import) }
                .bounds(centerX - 105, panelContentTop, 100, 20)
                .build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("Edit layout")) {
                minecraft.gui.setScreen(OverlayLayoutScreen(this))
            }
                .bounds(centerX + 5, panelContentTop, 100, 20)
                .build()
        )

        val overlays = ImageOverlays.overlays
        scroll = scroll.coerceIn(0, (overlays.size - MAX_VISIBLE).coerceAtLeast(0))
        overlays.drop(scroll).take(MAX_VISIBLE).forEachIndexed { i, overlay ->
            val rowY = listTop + i * ROW_HEIGHT
            var x = rowX + THUMB_SIZE + GAP + NAME_WIDTH + GAP

            addRenderableWidget(
                Button.builder(Component.literal(if (overlay.enabled) "On" else "Off")) {
                    overlay.enabled = !overlay.enabled
                    ImageOverlays.save()
                    rebuildWidgets()
                }.bounds(x, rowY, TOGGLE_WIDTH, 20).build()
            )
            x += TOGGLE_WIDTH + GAP

            addRenderableWidget(OpacitySlider(x, rowY, SLIDER_WIDTH, overlay))
            x += SLIDER_WIDTH + GAP

            addRenderableWidget(
                Button.builder(Component.literal("X")) {
                    ImageOverlays.remove(overlay)
                    rebuildWidgets()
                }.bounds(x, rowY, REMOVE_WIDTH, 20).build()
            )
        }

        addDoneButton()
    }

    override fun onFilesDrop(paths: List<Path>) = import(paths)

    private fun import(paths: List<Path>) {
        status = Component.literal("Importing ${paths.size} file(s)...")
        ImageOverlays.importImages(paths) { added, errors ->
            status = when {
                errors.isEmpty() -> Component.literal("Added $added overlay(s). Use Edit layout to place them.")
                else -> Component.literal("Added $added. Skipped: ${errors.joinToString("; ")}")
            }
            // Jump to the end so new overlays are visible.
            scroll = Int.MAX_VALUE
            if (minecraft.gui.screen() === this) rebuildWidgets()
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (ImageOverlays.overlays.size <= MAX_VISIBLE) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        scroll -= scrollY.toInt().coerceIn(-1, 1)
        rebuildWidgets()
        return true
    }

    override fun removed() {
        // Slider changes are applied live but only written here, to avoid a disk write per drag step.
        ImageOverlays.save()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)

        graphics.centeredText(
            font,
            Component.literal("Or drag PNG, JPG, GIF or BMP files onto this window."),
            width / 2, panelContentTop + 27, GRAY
        )

        val overlays = ImageOverlays.overlays
        if (overlays.isEmpty()) {
            graphics.centeredText(font, Component.literal("No overlays yet."), width / 2, listTop + 30, GRAY)
        }
        overlays.drop(scroll).take(MAX_VISIBLE).forEachIndexed { i, overlay ->
            val rowY = listTop + i * ROW_HEIGHT
            val image = MediaTextures.get(overlay.file)
            if (image != null) {
                // Fit the thumbnail inside the square, keeping the image's shape.
                val scale = THUMB_SIZE.toDouble() / maxOf(image.width, image.height)
                val w = (image.width * scale).roundToInt().coerceAtLeast(1)
                val h = (image.height * scale).roundToInt().coerceAtLeast(1)
                OverlayHud.draw(
                    graphics, overlay.copy(opacity = 1.0), image,
                    OverlayRect(rowX + (THUMB_SIZE - w) / 2, rowY + (THUMB_SIZE - h) / 2, w, h),
                )
            }

            val nameX = rowX + THUMB_SIZE + GAP
            val error = MediaTextures.error(overlay.file)
            val label = if (error != null) "${overlay.file} — can't load: $error" else overlay.file
            graphics.text(
                font,
                Component.literal(font.plainSubstrByWidth(label, NAME_WIDTH)),
                nameX, rowY + 6,
                if (error != null) RED else if (overlay.enabled) WHITE else GRAY,
            )
        }

        if (overlays.size > MAX_VISIBLE) {
            graphics.centeredText(
                font,
                Component.literal("${scroll + 1}-${minOf(scroll + MAX_VISIBLE, overlays.size)} of ${overlays.size} (scroll for more)"),
                width / 2, listTop + MAX_VISIBLE * ROW_HEIGHT + 2, GRAY
            )
        }
        status?.let {
            graphics.centeredText(font, it, width / 2, panelFooterTop - 14, GRAY)
        }
    }

    private class OpacitySlider(x: Int, y: Int, width: Int, private val overlay: ImageOverlay) :
        AbstractSliderButton(x, y, width, 20, Component.empty(), overlay.opacity) {

        init {
            updateMessage()
        }

        override fun updateMessage() {
            message = Component.literal("Opacity: ${(value * 100).roundToInt()}%")
        }

        override fun applyValue() {
            overlay.opacity = value
        }

        override fun onRelease(event: MouseButtonEvent) {
            super.onRelease(event)
            ImageOverlays.save()
        }
    }
}
