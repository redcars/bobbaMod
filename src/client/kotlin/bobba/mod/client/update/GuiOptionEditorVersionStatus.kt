package bobba.mod.client.update

import io.github.notenoughupdates.moulconfig.GuiTextures
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption

class GuiOptionEditorVersionStatus(option: ProcessedOption) : GuiOptionEditor(option) {

    private companion object {
        const val ARGB_GREEN = 0xFF55FF55.toInt()
        const val ARGB_RED = 0xFFFF5555.toInt()
        const val ARGB_AQUA = 0xFF55FFFF.toInt()
        const val ARGB_YELLOW = 0xFFFFFF55.toInt()
        const val ARGB_GRAY = 0xFFAAAAAA.toInt()
        const val BUTTON_TEXT_COLOR = 0xFF303030.toInt()

        const val BUTTON_WIDTH = 70
        const val BUTTON_HEIGHT = 16
        const val BUTTON_PADDING_X = 10
    }

    private fun buttonRect(x: Int, y: Int, width: Int): IntArray {
        val bx = x + width - BUTTON_WIDTH - BUTTON_PADDING_X
        val by = y + (height - BUTTON_HEIGHT) / 2
        return intArrayOf(bx, by, bx + BUTTON_WIDTH, by + BUTTON_HEIGHT)
    }

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        super.render(context, x, y, width)

        val fr = context.minecraft.defaultFontRenderer
        val state = UpdateChecker.state
        val current = UpdateChecker.currentVersion()
        val latest = UpdateChecker.latestVersion

        val (statusText, statusColor) = when (state) {
            UpdateChecker.State.IDLE -> "v$current" to ARGB_GRAY
            UpdateChecker.State.CHECKING -> "v$current  (checking…)" to ARGB_YELLOW
            UpdateChecker.State.UP_TO_DATE -> "v$current  (up to date)" to ARGB_GREEN
            UpdateChecker.State.UPDATE_AVAILABLE -> "v$current  →  v${latest ?: "?"}" to ARGB_AQUA
            UpdateChecker.State.ERROR -> "v$current  (${UpdateChecker.lastError ?: "error"})" to ARGB_RED
        }

        val buttonText = when (state) {
            UpdateChecker.State.CHECKING -> "…"
            UpdateChecker.State.UPDATE_AVAILABLE -> "Show link"
            else -> "Check now"
        }

        val (bx, by, bxEnd, _) = buttonRect(x, y, width)

        // Native button background + dark centered text
        context.drawTexturedRect(
            GuiTextures.BUTTON,
            bx.toFloat(), by.toFloat(),
            BUTTON_WIDTH.toFloat(), BUTTON_HEIGHT.toFloat()
        )
        context.drawStringCenteredScaledMaxWidth(
            StructuredText.of(buttonText),
            fr,
            bx + BUTTON_WIDTH / 2f,
            by + BUTTON_HEIGHT / 2f,
            false,
            BUTTON_WIDTH - 6,
            BUTTON_TEXT_COLOR
        )

        // Status text fills the middle area between the title (left 1/3) and the button (right)
        val statusAreaStart = x + width / 3 + 10
        val statusAreaEnd = bx - 10
        val statusAreaWidth = (statusAreaEnd - statusAreaStart).coerceAtLeast(20)
        context.drawStringCenteredScaledMaxWidth(
            StructuredText.of(statusText),
            fr,
            statusAreaStart + statusAreaWidth / 2f,
            y + height / 2f,
            true,
            statusAreaWidth,
            statusColor
        )
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, mouseEvent: MouseEvent): Boolean {
        if (mouseEvent !is MouseEvent.Click) return false
        if (!mouseEvent.mouseState || mouseEvent.mouseButton != 0) return false

        val (bx, by, bxEnd, byEnd) = buttonRect(x, y, width)
        if (mouseX !in bx until bxEnd || mouseY !in by until byEnd) return false

        when (UpdateChecker.state) {
            UpdateChecker.State.UPDATE_AVAILABLE -> UpdateChecker.openLatest()
            UpdateChecker.State.CHECKING -> {}
            else -> UpdateChecker.forceCheck()
        }
        return true
    }

    override fun getHeight(): Int = 45

    override fun fulfillsSearch(word: String): Boolean {
        return super.fulfillsSearch(word) || word in "version" || word in "update"
    }
}

private operator fun IntArray.component1() = this[0]
private operator fun IntArray.component2() = this[1]
private operator fun IntArray.component3() = this[2]
private operator fun IntArray.component4() = this[3]
