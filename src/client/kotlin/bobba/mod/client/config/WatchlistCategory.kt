package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WatchlistCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Warn on party join", desc = "Show a warning when a watchlisted player joins your party.")
    @ConfigEditorBoolean
    var warnOnPartyJoin: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "Warn on server presence", desc = "Show a warning when a watchlisted player is on the same server as you.")
    @ConfigEditorBoolean
    var warnOnServerPresence: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "Play sound on warning", desc = "Play a ding sound when a warning fires.")
    @ConfigEditorBoolean
    var playSound: Boolean = true
}
