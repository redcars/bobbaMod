package bobba.mod.client.config

import bobba.mod.client.watchlist.gui.WatchlistEditorScreen
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WatchlistCategory {
    // Discovered by MoulConfig via reflection; the IDE can't see the usage.
    @Suppress("unused")
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Manage watchlist", desc = "Open the editor to add or remove watched players.")
    @ConfigEditorButton(buttonText = "Open")
    var openWatchlistEditor: Runnable = Runnable { WatchlistEditorScreen.open() }

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
