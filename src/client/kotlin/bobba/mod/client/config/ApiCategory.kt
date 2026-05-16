package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ApiCategory {
    @JvmField
    @Expose
    @ConfigOption(
        name = "Hypixel API key",
        desc = "Your Hypixel API key from developer.hypixel.net. Optional — ranks of players seen in chat are auto-detected for free; the key is only needed to resolve ranks for players you haven't met yet."
    )
    @ConfigEditorText
    var hypixelApiKey: String = ""
}
