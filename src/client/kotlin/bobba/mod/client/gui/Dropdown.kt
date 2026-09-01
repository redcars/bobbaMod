package bobba.mod.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.network.chat.Component

/** One selectable entry of a [Dropdown]; [value] is handed back to the screen on selection. */
data class DropdownOption<T>(val value: T, val label: Component)

/**
 * An overlay option list anchored to a button, owned and driven by the screen that opened it.
 *
 * It is deliberately not a widget: vanilla widgets render in the order they were added, so a popup
 * built from them would be painted under the rows it covers. The owning screen instead keeps at
 * most one open dropdown, renders it after its widgets (see [render]) and routes input to it before
 * the widgets ([mouseClicked], [mouseScrolled]).
 *
 * The list opens below its anchor when there is room and flips above it otherwise, so rows near the
 * bottom of the screen stay usable; longer option lists scroll.
 */
class Dropdown<T>(
    private val anchorX: Int,
    private val anchorY: Int,
    private val width: Int,
    anchorHeight: Int,
    private val options: List<DropdownOption<T>>,
    private val selected: T,
    screenHeight: Int,
    private val onSelect: (T) -> Unit,
) {
    private companion object {
        const val ROW_HEIGHT = 14
        const val MAX_ROWS = 8
        const val TEXT_INSET = 5
        const val SCROLLBAR_WIDTH = 2

        val BACKGROUND_COLOR = 0xF0121212.toInt()
        val BORDER_COLOR = 0xFF808080.toInt()
        val HOVER_COLOR = 0xFF3A5A80.toInt()
        val SELECTED_COLOR = 0xFF303030.toInt()
        val SCROLLBAR_COLOR = 0xFF808080.toInt()
        val TEXT_COLOR = 0xFFFFFFFF.toInt()
    }

    private val visibleRows: Int
    private val listTop: Int
    private var scroll = 0

    init {
        val belowTop = anchorY + anchorHeight + 1
        val roomBelow = ((screenHeight - belowTop - 2) / ROW_HEIGHT).coerceAtLeast(0)
        val roomAbove = ((anchorY - 3) / ROW_HEIGHT).coerceAtLeast(0)
        val wanted = options.size.coerceAtMost(MAX_ROWS)

        // Prefer opening downwards; flip above the anchor only when that side has more room.
        val placeBelow = roomBelow >= wanted || roomBelow >= roomAbove
        visibleRows = (if (placeBelow) roomBelow else roomAbove).coerceIn(1, wanted)
        listTop = if (placeBelow) belowTop else anchorY - 1 - visibleRows * ROW_HEIGHT

        // Open scrolled to the current selection so it is visible in a long list.
        val selectedIndex = options.indexOfFirst { it.value == selected }
        if (selectedIndex >= visibleRows) scroll = clampScroll(selectedIndex - visibleRows + 1)
    }

    private val listBottom get() = listTop + visibleRows * ROW_HEIGHT
    private val maxScroll get() = (options.size - visibleRows).coerceAtLeast(0)

    private fun clampScroll(value: Int) = value.coerceIn(0, maxScroll)

    fun render(graphics: GuiGraphicsExtractor, font: Font, mouseX: Int, mouseY: Int) {
        // Draw above the screen's widgets, which have already been submitted at this point.
        graphics.nextStratum()

        graphics.fill(anchorX - 1, listTop - 1, anchorX + width + 1, listBottom + 1, BORDER_COLOR)
        graphics.fill(anchorX, listTop, anchorX + width, listBottom, BACKGROUND_COLOR)

        for (row in 0 until visibleRows) {
            val option = options[scroll + row]
            val rowTop = listTop + row * ROW_HEIGHT
            val hovered = mouseX >= anchorX && mouseX < anchorX + width &&
                mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT
            when {
                hovered -> graphics.fill(anchorX, rowTop, anchorX + width, rowTop + ROW_HEIGHT, HOVER_COLOR)
                option.value == selected ->
                    graphics.fill(anchorX, rowTop, anchorX + width, rowTop + ROW_HEIGHT, SELECTED_COLOR)
            }
            graphics.text(font, option.label, anchorX + TEXT_INSET, rowTop + 3, TEXT_COLOR, false)
        }

        if (maxScroll > 0) {
            val trackHeight = visibleRows * ROW_HEIGHT
            val thumbHeight = (trackHeight * visibleRows / options.size).coerceAtLeast(ROW_HEIGHT / 2)
            val thumbTop = listTop + (trackHeight - thumbHeight) * scroll / maxScroll
            val thumbLeft = anchorX + width - SCROLLBAR_WIDTH - 1
            graphics.fill(thumbLeft, thumbTop, thumbLeft + SCROLLBAR_WIDTH, thumbTop + thumbHeight, SCROLLBAR_COLOR)
        }
    }

    /** Selects the clicked option; returns false when the click landed outside the list. */
    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        if (mouseX < anchorX || mouseX >= anchorX + width) return false
        if (mouseY < listTop || mouseY >= listBottom) return false
        val index = scroll + ((mouseY - listTop) / ROW_HEIGHT).toInt()
        val option = options.getOrNull(index) ?: return false
        AbstractWidget.playButtonClickSound(Minecraft.getInstance().soundManager)
        onSelect(option.value)
        return true
    }

    fun mouseScrolled(scrollY: Double): Boolean {
        if (maxScroll == 0 || scrollY == 0.0) return false
        scroll = clampScroll(scroll + if (scrollY > 0) -1 else 1)
        return true
    }
}
