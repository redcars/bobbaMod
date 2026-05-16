package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.annotations.Category

class BobbaConfig : Config() {
    @JvmField
    @Expose
    @Category(name = "Watchlist", desc = "Settings for the player watchlist")
    var watchlist: WatchlistCategory = WatchlistCategory()
}
