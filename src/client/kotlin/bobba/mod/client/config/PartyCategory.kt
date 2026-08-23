package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PartyCategory {
    @JvmField
    @Expose
    @ConfigOption(
        name = "Auto-kick watchlisted players",
        desc = "Automatically run /party kick on a watchlisted player the moment they join your party. Off by default."
    )
    @ConfigEditorBoolean
    var autoKickWatchlisted: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(
        name = "Quick-kick button on join",
        desc = "Show a clickable [Kick] button in chat whenever any player joins your party, so you can kick them in one click even if they aren't on the watchlist. Off by default."
    )
    @ConfigEditorBoolean
    var quickKickButton: Boolean = false
}
