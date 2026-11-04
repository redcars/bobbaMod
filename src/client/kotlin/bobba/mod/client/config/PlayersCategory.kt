package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PlayersCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Change player looks", desc = "Use the size and opacity settings below. Only changes what you see: hitboxes and other players' screens stay the same.")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "Your player", desc = "How you look in third person (F5).")
    @Accordion
    var self: PlayerLook = PlayerLook()

    @JvmField
    @Expose
    @ConfigOption(name = "Other players", desc = "How everyone else looks.")
    @Accordion
    var others: PlayerLook = PlayerLook()

    @JvmField
    @Expose
    @ConfigOption(name = "Also change NPCs", desc = "Hypixel's NPCs are players too. Off = NPCs keep their normal look.")
    @ConfigEditorBoolean
    var includeNpcs: Boolean = false
}

class PlayerLook {
    @JvmField
    @Expose
    @ConfigOption(name = "Size", desc = "1 = normal size.")
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3f, minStep = 0.05f)
    var size: Float = 1f

    @JvmField
    @Expose
    @ConfigOption(name = "Model opacity (%)", desc = "Skin and cape. 0 hides them; armor and held items still show.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 5f)
    var modelOpacity: Int = 100

    @JvmField
    @Expose
    @ConfigOption(name = "Armor opacity (%)", desc = "Armor and elytra. 0 hides them.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 5f)
    var armorOpacity: Int = 100
}
