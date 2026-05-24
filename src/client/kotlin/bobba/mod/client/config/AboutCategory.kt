package bobba.mod.client.config

import bobba.mod.client.update.ConfigEditorVersionStatus
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AboutCategory {
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Version", desc = "")
    @ConfigEditorVersionStatus
    var versionStatus: String = ""

    @JvmField
    @Expose
    @ConfigOption(name = "Check for updates on launch", desc = "Check GitHub for a newer BobbaMod release once per session.")
    @ConfigEditorBoolean
    var checkForUpdates: Boolean = true
}
