package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category

class BobbaConfig : Config() {
    @JvmField
    @Expose
    @Category(name = "Watchlist", desc = "Settings for the player watchlist")
    var watchlist: WatchlistCategory = WatchlistCategory()

    @JvmField
    @Expose
    @Category(name = "Keybinds", desc = "Bind keys to commands or chat messages")
    var keybinds: KeybindsCategory = KeybindsCategory()

    @JvmField
    @Expose
    @Category(name = "Party", desc = "Party-wide automations independent of the watchlist")
    var party: PartyCategory = PartyCategory()

    @JvmField
    @Expose
    @Category(name = "API", desc = "External API integration settings")
    var api: ApiCategory = ApiCategory()

    @JvmField
    @Expose
    @Category(name = "About", desc = "Version info and update settings")
    var about: AboutCategory = AboutCategory()
}
