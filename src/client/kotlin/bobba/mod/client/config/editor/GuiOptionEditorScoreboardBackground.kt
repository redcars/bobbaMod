package bobba.mod.client.config.editor

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.media.MediaDropTarget
import bobba.mod.client.media.MediaFilePicker
import bobba.mod.client.media.MediaLibrary
import bobba.mod.client.media.MediaTextures
import bobba.mod.client.scoreboard.CustomScoreboard
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import java.nio.file.Path

/** Background picker inside the Scoreboard tab: preview, choose and remove. */
class GuiOptionEditorScoreboardBackground(option: ProcessedOption) : GuiOptionEditor(option) {

    private companion object {
        const val PAD = 8
        const val PREVIEW = 56
        const val BUTTON_WIDTH = 96
        const val BUTTON_HEIGHT = 18
        const val GAP = 6
        const val LINE = 12
    }

    private var status: String? = null

    private val imageName: String? get() = ConfigManager.instance.scoreboard.backgroundImage

    private fun preview(x: Int, y: Int) = Box(x + PAD, y + PAD, PREVIEW, PREVIEW)
    private fun textX(x: Int) = x + PAD + PREVIEW + 12
    private fun chooseButton(x: Int, y: Int) = Box(textX(x), y + PAD + 2 * LINE + 4, BUTTON_WIDTH, BUTTON_HEIGHT)
    private fun removeButton(x: Int, y: Int) = Box(textX(x) + BUTTON_WIDTH + GAP, y + PAD + 2 * LINE + 4, BUTTON_WIDTH, BUTTON_HEIGHT)

    override fun getHeight(): Int = PAD + PREVIEW + PAD + (if (status != null) LINE else 0)

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        MediaDropTarget.offer(::choose)
        val fr = context.minecraft.defaultFontRenderer
        context.drawDarkRect(x, y, width, height, true)

        val preview = preview(x, y)
        EditorDraw.outline(context, preview)
        val name = imageName
        val image = name?.let { MediaTextures.get(it) }
        when {
            name == null -> EditorDraw.text(context, fr, "None", preview.x + 16, preview.y + PREVIEW / 2 - 4, EditorDraw.GRAY)
            image != null -> EditorDraw.image(context, image, EditorDraw.fit(image, preview))
        }

        val tx = textX(x)
        val textWidth = x + width - PAD - tx
        EditorDraw.text(context, fr, "Background image", tx, y + PAD, EditorDraw.WHITE)
        val error = name?.let { MediaTextures.error(it) }
        when {
            error != null -> EditorDraw.text(context, fr, "$name — can't load: $error", tx, y + PAD + LINE, EditorDraw.RED, textWidth)
            name != null -> EditorDraw.text(context, fr, name, tx, y + PAD + LINE, EditorDraw.GRAY, textWidth)
            else -> EditorDraw.text(context, fr, "None chosen", tx, y + PAD + LINE, EditorDraw.GRAY, textWidth)
        }
        EditorDraw.button(context, fr, chooseButton(x, y), "Choose image...")
        EditorDraw.button(context, fr, removeButton(x, y), "Remove", enabled = name != null)
        EditorDraw.text(
            context, fr, "Or drag a picture or GIF onto this window. It's cropped to fill the box.",
            tx, chooseButton(x, y).bottom + 6, EditorDraw.GRAY, textWidth,
        )

        status?.let { EditorDraw.text(context, fr, it, x + PAD, y + PAD + PREVIEW + 4, EditorDraw.GRAY, width - 2 * PAD) }
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, mouseEvent: MouseEvent): Boolean {
        if (mouseEvent !is MouseEvent.Click || !mouseEvent.mouseState || mouseEvent.mouseButton != 0) return false
        when {
            chooseButton(x, y).contains(mouseX, mouseY) -> MediaFilePicker.pickImages(::choose)
            removeButton(x, y).contains(mouseX, mouseY) && imageName != null -> {
                CustomScoreboard.setBackground(null)
                status = null
            }
            else -> return false
        }
        return true
    }

    private fun choose(paths: List<Path>) {
        status = "Importing..."
        // Only one background: the first file wins.
        MediaLibrary.importImages(paths.take(1)) { names, errors ->
            names.firstOrNull()?.let { CustomScoreboard.setBackground(it) }
            status = when {
                errors.isNotEmpty() -> "Skipped: ${errors.joinToString("; ")}"
                names.isNotEmpty() -> "The scoreboard background is now ${names.first()}."
                else -> null
            }
        }
    }

    override fun fulfillsSearch(word: String): Boolean =
        super.fulfillsSearch(word) || word in "scoreboard" || word in "background" || word in "image" || word in "gif"
}
