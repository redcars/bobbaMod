package bobba.mod.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Shared base for BobbaMod screens.
 *
 * Provides a darkened backdrop and a centered content "panel" with a subtle border so the screen
 * reads cleanly against the game world. Subclasses position widgets using the [panelLeft],
 * [panelTop], [panelRight], [panelBottom], [panelContentTop] helpers so their layout adapts to
 * the window size, and call [addDoneButton] for the standard close button.
 */
abstract class BobbaScreen(
    title: Component,
    private val parent: Screen?,
) : Screen(title) {

    /** Width of the centered content panel, in scaled GUI pixels. Override per screen. */
    protected open val panelWidth: Int = 380

    /** Vertical inset of the panel from the top/bottom of the screen. */
    protected open val panelMargin: Int = 25

    protected val panelLeft: Int get() = (width - panelWidth) / 2
    protected val panelRight: Int get() = panelLeft + panelWidth
    protected val panelTop: Int get() = panelMargin
    protected val panelBottom: Int get() = height - panelMargin

    /** Y coordinate where subclass content should begin (below the title). */
    protected val panelContentTop: Int get() = panelTop + 28

    /** Y coordinate where the bottom "Done" row sits. */
    protected val panelFooterTop: Int get() = panelBottom - 28

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(graphics, mouseX, mouseY, delta)
        graphics.fill(0, 0, width, height, OVERLAY_COLOR)
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL_COLOR)
        graphics.fill(panelLeft - 1, panelTop - 1, panelRight + 1, panelTop, BORDER_COLOR)
        graphics.fill(panelLeft - 1, panelBottom, panelRight + 1, panelBottom + 1, BORDER_COLOR)
        graphics.fill(panelLeft - 1, panelTop - 1, panelLeft, panelBottom + 1, BORDER_COLOR)
        graphics.fill(panelRight, panelTop - 1, panelRight + 1, panelBottom + 1, BORDER_COLOR)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(graphics, mouseX, mouseY, delta)
        graphics.drawCenteredString(font, title, width / 2, panelTop + 9, TITLE_COLOR)
    }

    override fun onClose() {
        Minecraft.getInstance().setScreen(parent)
    }

    protected fun addDoneButton() {
        addRenderableWidget(
            Button.builder(Component.literal("Done")) { onClose() }
                .bounds(width / 2 - 50, panelFooterTop, 100, 20)
                .build()
        )
    }

    private companion object {
        val OVERLAY_COLOR = 0xB0000000.toInt()
        val PANEL_COLOR = 0xD0101010.toInt()
        val BORDER_COLOR = 0xFF606060.toInt()
        val TITLE_COLOR = 0xFFFFFFFF.toInt()
    }
}
