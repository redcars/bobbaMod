package bobba.mod.client.config

import bobba.mod.client.keybinds.gui.KeybindEditorScreen
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class KeybindsCategory {
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(
        name = "Manage keybinds",
        desc = "Open the editor to bind keys to commands or chat messages. You can also use /keybinds add|remove|list."
    )
    @ConfigEditorButton(buttonText = "Open")
    var openKeybindEditor: Runnable = Runnable { KeybindEditorScreen.open() }
}
