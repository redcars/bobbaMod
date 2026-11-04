package bobba.mod.client.config

import bobba.mod.client.config.editor.ConfigEditorScoreboardBackground
import bobba.mod.client.config.editor.ConfigEditorScoreboardLines
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ScoreboardCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Custom scoreboard", desc = "Draw the sidebar scoreboard with the settings below. Turn off to get Minecraft's own scoreboard back.")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "Size", desc = "Scale of the scoreboard. 1 = normal size. It stays on the right edge of the screen.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 2f, minStep = 0.05f)
    var scale: Float = 1f

    @JvmField
    @Expose
    @ConfigOption(name = "Opacity (%)", desc = "How visible the whole scoreboard is, text included.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 5f)
    var opacity: Int = 100

    @JvmField
    @Expose
    @ConfigOption(name = "Background darkness (%)", desc = "How dark the box behind the text is. Minecraft's default is 30. Set 0 to see only your background image.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 5f)
    var backgroundOpacity: Int = 30

    @JvmField
    @Expose
    @ConfigOption(name = "Hide red numbers", desc = "Hide the score numbers on the right of each line.")
    @ConfigEditorBoolean
    var hideScoreNumbers: Boolean = false

    // The editor reads and writes backgroundImage; this field only gives it a slot.
    @Suppress("unused")
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Background image", desc = "A picture or GIF behind the scoreboard.")
    @ConfigEditorScoreboardBackground
    var backgroundEditor: String = ""

    @JvmField
    @Expose
    @ConfigOption(name = "Fit box to image", desc = "Stretch the scoreboard's box to the background image's shape, so the whole image shows instead of being cropped.")
    @ConfigEditorBoolean
    var fitToImage: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Background image opacity (%)", desc = "How visible the background image is.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 5f)
    var backgroundImageOpacity: Int = 100

    // The editor reads and writes hiddenLines; this field only gives it a slot.
    @Suppress("unused")
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Lines", desc = "Hide lines of the scoreboard.")
    @ConfigEditorScoreboardLines
    var linesEditor: String = ""

    /** Line keys from [bobba.mod.client.scoreboard.CustomScoreboard.key]; set by the lines editor. */
    @JvmField
    @Expose
    var hiddenLines: MutableList<String> = mutableListOf()

    /** Stored name in the media library; set by the background editor. */
    @JvmField
    @Expose
    var backgroundImage: String? = null
}
