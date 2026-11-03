package bobba.mod.client.config

import bobba.mod.client.config.editor.ConfigEditorOverlayList
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class OverlaysCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Show image overlays", desc = "Draw your image overlays on the HUD. Turn off to hide all of them at once.")
    @ConfigEditorBoolean
    var showImageOverlays: Boolean = true

    // The editor reads and writes ImageOverlays directly; this field only gives it a slot.
    @Suppress("unused")
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Image overlays", desc = "Pictures and GIFs drawn on your screen.")
    @ConfigEditorOverlayList
    var overlayList: String = ""
}
