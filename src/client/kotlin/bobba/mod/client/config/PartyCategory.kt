package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PartyCategory {
    @JvmField
    @Expose
    @ConfigOption(
        name = "Suggest kick on party join",
        desc = "When a player joins your party and their rank matches the filters below, show a clickable chat suggestion to run /party kick. The kick only runs if you click."
    )
    @ConfigEditorBoolean
    var autoKickFromParty: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(
        name = "Rank filters",
        desc = "Only show a kick suggestion for joining players whose rank matches one of the selected tiers. Staff and YouTube ranks are never suggested."
    )
    @Accordion
    var autoKickFilters: AutoKickFilters = AutoKickFilters()
}
