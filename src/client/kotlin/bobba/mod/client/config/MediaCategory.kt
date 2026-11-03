package bobba.mod.client.config

import bobba.mod.client.overlay.gui.OverlayManagerScreen
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MediaCategory {
    // Discovered by MoulConfig via reflection; the IDE can't see the usage.
    @Suppress("unused")
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(
        name = "Image overlays",
        desc = "Import pictures or GIFs from your PC and place them on your screen. You can also use /bobba overlays."
    )
    @ConfigEditorButton(buttonText = "Open")
    var openOverlays: Runnable = Runnable { OverlayManagerScreen.open() }

    @JvmField
    @Expose
    @ConfigOption(name = "Show image overlays", desc = "Draw your image overlays on the HUD. Turn off to hide all of them at once.")
    @ConfigEditorBoolean
    var showImageOverlays: Boolean = true
}
