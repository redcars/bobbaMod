package bobba.mod.client.config

import bobba.mod.client.update.UpdateChecker
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AboutCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Check for updates on launch", desc = "Check GitHub for a newer BobbaMod release once per session.")
    @ConfigEditorBoolean
    var checkForUpdates: Boolean = true

    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Check now", desc = "Run an update check immediately.")
    @ConfigEditorButton(buttonText = "Check")
    var checkNow: Runnable = Runnable { UpdateChecker.forceCheck() }

    @JvmField
    @Expose
    @ConfigOption(
        name = "Enable debug commands",
        desc = "Register /testparty and /testpresence for simulating events. Requires a restart to take effect."
    )
    @ConfigEditorBoolean
    var debugCommands: Boolean = false
}
