package bobba.mod.client.config.editor

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.scoreboard.CustomScoreboard
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption

/**
 * Lists the lines on the scoreboard right now with a Hide/Show button each, then any hidden lines
 * that aren't on this scoreboard (other islands, other modes) so they can be shown again.
 */
class GuiOptionEditorScoreboardLines(option: ProcessedOption) : GuiOptionEditor(option) {

    private companion object {
        const val PAD = 8
        const val LINE = 12
        const val ROW = 16
        const val BUTTON_WIDTH = 52
        const val BUTTON_HEIGHT = 14
    }

    private class Row(val key: String, val label: String, val hidden: Boolean)

    private class Layout(val live: List<Row>?, val saved: List<Row>)

    private fun layout(): Layout {
        val hidden = ConfigManager.instance.scoreboard.hiddenLines
        val objective = CustomScoreboard.currentObjective()
        val live = objective?.let { obj ->
            val rows = mutableListOf(
                Row(CustomScoreboard.TITLE_KEY, "Title: " + CustomScoreboard.plain(obj.displayName.string), CustomScoreboard.TITLE_KEY in hidden)
            )
            for (line in CustomScoreboard.lines(obj)) {
                // Lines that share a key (all blank lines, say) are one switch.
                if (rows.any { it.key == line.key }) continue
                val text = CustomScoreboard.plain(line.name.string).ifEmpty { "(blank lines)" }
                rows += Row(line.key, text, line.key in hidden)
            }
            rows
        }
        val liveKeys = live?.mapTo(HashSet()) { it.key } ?: emptySet()
        val saved = hidden.filter { it !in liveKeys }.map { Row(it, describe(it), true) }
        return Layout(live, saved)
    }

    private fun describe(key: String) = when (key) {
        CustomScoreboard.TITLE_KEY -> "Title"
        "" -> "(blank lines)"
        else -> key
    }

    private fun liveTop(y: Int) = y + PAD + 2 * LINE + 4
    private fun liveHeight(layout: Layout) = layout.live?.let { it.size * ROW } ?: LINE
    private fun savedTop(y: Int, layout: Layout) = liveTop(y) + liveHeight(layout) + 6 + LINE

    private fun button(x: Int, width: Int, rowY: Int) = Box(x + width - PAD - BUTTON_WIDTH, rowY + 1, BUTTON_WIDTH, BUTTON_HEIGHT)

    override fun getHeight(): Int {
        val layout = layout()
        var h = liveTop(0) + liveHeight(layout)
        if (layout.saved.isNotEmpty()) h += 6 + LINE + layout.saved.size * ROW
        return h + PAD
    }

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        val fr = context.minecraft.defaultFontRenderer
        context.drawDarkRect(x, y, width, height, true)
        val layout = layout()
        val textWidth = width - 3 * PAD - BUTTON_WIDTH

        EditorDraw.text(context, fr, "Scoreboard lines", x + PAD, y + PAD, EditorDraw.WHITE)
        EditorDraw.text(
            context, fr, "Hidden lines stay hidden wherever they show up. # = any number.",
            x + PAD, y + PAD + LINE, EditorDraw.GRAY, width - 2 * PAD,
        )

        val live = layout.live
        if (live == null) {
            EditorDraw.text(context, fr, "No scoreboard right now. Join a world that has one.", x + PAD, liveTop(y) + 3, EditorDraw.GRAY, width - 2 * PAD)
        } else {
            live.forEachIndexed { i, row -> drawRow(context, fr, x, width, liveTop(y) + i * ROW, row, textWidth, if (row.hidden) "Show" else "Hide") }
        }

        if (layout.saved.isNotEmpty()) {
            val top = savedTop(y, layout)
            EditorDraw.text(context, fr, "Hidden, not on this scoreboard", x + PAD, top - LINE, EditorDraw.WHITE)
            layout.saved.forEachIndexed { i, row -> drawRow(context, fr, x, width, top + i * ROW, row, textWidth, "Show") }
        }
    }

    private fun drawRow(
        context: RenderContext,
        fr: io.github.notenoughupdates.moulconfig.common.IFontRenderer,
        x: Int, width: Int, rowY: Int, row: Row, textWidth: Int, action: String,
    ) {
        val label = if (row.hidden) "${row.label}  (hidden)" else row.label
        EditorDraw.text(context, fr, label, x + PAD, rowY + 4, if (row.hidden) EditorDraw.GRAY else EditorDraw.WHITE, textWidth)
        EditorDraw.button(context, fr, button(x, width, rowY), action)
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, mouseEvent: MouseEvent): Boolean {
        if (mouseEvent !is MouseEvent.Click || !mouseEvent.mouseState || mouseEvent.mouseButton != 0) return false
        val layout = layout()
        layout.live?.forEachIndexed { i, row ->
            if (button(x, width, liveTop(y) + i * ROW).contains(mouseX, mouseY)) {
                CustomScoreboard.setHidden(row.key, !row.hidden)
                return true
            }
        }
        val top = savedTop(y, layout)
        layout.saved.forEachIndexed { i, row ->
            if (button(x, width, top + i * ROW).contains(mouseX, mouseY)) {
                CustomScoreboard.setHidden(row.key, false)
                return true
            }
        }
        return false
    }

    override fun fulfillsSearch(word: String): Boolean =
        super.fulfillsSearch(word) || word in "scoreboard" || word in "lines" || word in "hide" || word in "sidebar"
}
