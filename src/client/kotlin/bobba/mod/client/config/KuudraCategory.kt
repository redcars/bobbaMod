package bobba.mod.client.config

import bobba.mod.client.config.editor.ConfigEditorKuudraFace
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class KuudraCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Replace Kuudra's face", desc = "Draw your chosen image or GIF over the Kuudra boss's face in Kuudra's Hollow.")
    @ConfigEditorBoolean
    var faceOverride: Boolean = true

    // The editor reads and writes faceImage; this field only gives it a slot.
    @Suppress("unused")
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Face image", desc = "The picture or GIF put on Kuudra's face.")
    @ConfigEditorKuudraFace
    var faceEditor: String = ""

    @JvmField
    @Expose
    @ConfigOption(
        name = "Hide lair walls and roof",
        desc = "In Kuudra's Hollow, only draw the arena and the lava around it, so your sky shows through. Only changes what you see: the blocks are still there.\n" +
            "§cHypixel's Allowed Modifications guide forbids making solid blocks see-through. Use at your own risk."
    )
    @ConfigEditorBoolean
    var hideLair: Boolean = false

    /** Stored name in the media library; set by the face editor, not shown as an option. */
    @JvmField
    @Expose
    var faceImage: String? = null
}
