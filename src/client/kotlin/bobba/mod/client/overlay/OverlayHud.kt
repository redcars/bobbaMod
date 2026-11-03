package bobba.mod.client.overlay

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.media.LoadedImage
import bobba.mod.client.media.MediaTextures
import bobba.mod.client.overlay.gui.OverlayLayoutScreen
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt

/** On-screen bounds of an overlay, in scaled GUI pixels. */
data class OverlayRect(val x: Int, val y: Int, val width: Int, val height: Int) {
    val right get() = x + width
    val bottom get() = y + height

    fun contains(px: Double, py: Double) = px >= x && px < right && py >= y && py < bottom
}

object OverlayHud {
    private val ID = Identifier.fromNamespaceAndPath("bobbamod", "image_overlays")
    const val MIN_SIZE = 8

    fun init() {
        // Below chat so messages stay readable over a big overlay.
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID) { graphics, _ ->
            if (!ConfigManager.instance.media.showImageOverlays) return@attachElementBefore
            // The layout editor draws them itself, with outlines and handles.
            if (Minecraft.getInstance().gui.screen() is OverlayLayoutScreen) return@attachElementBefore
            for (overlay in ImageOverlays.overlays) {
                if (!overlay.enabled) continue
                val image = MediaTextures.get(overlay.file) ?: continue
                draw(graphics, overlay, image, rect(overlay, image, graphics.guiWidth(), graphics.guiHeight()))
            }
        }
    }

    fun rect(overlay: ImageOverlay, image: LoadedImage, screenWidth: Int, screenHeight: Int): OverlayRect {
        val width = (overlay.width * screenWidth).roundToInt().coerceAtLeast(MIN_SIZE)
        val height = (width.toDouble() * image.height / image.width).roundToInt().coerceAtLeast(1)
        return OverlayRect(
            (overlay.x * screenWidth).roundToInt(),
            (overlay.y * screenHeight).roundToInt(),
            width,
            height,
        )
    }

    fun draw(graphics: GuiGraphicsExtractor, overlay: ImageOverlay, image: LoadedImage, rect: OverlayRect) {
        val alpha = (overlay.opacity * 255).roundToInt().coerceIn(0, 255)
        if (alpha == 0) return
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            image.frameAt(System.currentTimeMillis()),
            rect.x, rect.y, 0f, 0f,
            rect.width, rect.height,
            image.width, image.height,
            image.width, image.height,
            (alpha shl 24) or 0xFFFFFF,
        )
    }
}
