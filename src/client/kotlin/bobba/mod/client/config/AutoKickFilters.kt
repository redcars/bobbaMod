package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AutoKickFilters {
    @JvmField
    @Expose
    @ConfigOption(name = "Unranked players", desc = "Suggest kicking players with no rank.")
    @ConfigEditorBoolean
    var kickUnranked: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "VIP", desc = "Suggest kicking [VIP] players.")
    @ConfigEditorBoolean
    var kickVip: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "VIP+", desc = "Suggest kicking [VIP+] players.")
    @ConfigEditorBoolean
    var kickVipPlus: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "MVP", desc = "Suggest kicking [MVP] players.")
    @ConfigEditorBoolean
    var kickMvp: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "MVP+", desc = "Suggest kicking [MVP+] players.")
    @ConfigEditorBoolean
    var kickMvpPlus: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "MVP++", desc = "Suggest kicking [MVP++] players.")
    @ConfigEditorBoolean
    var kickMvpPlusPlus: Boolean = true
}
