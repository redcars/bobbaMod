package bobba.mod.client.config

import bobba.mod.client.config.editor.ConfigEditorRandomClips
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RandomMediaCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Play random media", desc = "Play your clips below at random times while you're in a world. Needs the WATERMeDIA and WATERMeDIA: Binaries mods.")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @JvmField
    @Expose
    @ConfigOption(name = "Cooldown (seconds)", desc = "Minimum time between one clip ending and the next one starting. 0 = no cooldown.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 600f, minStep = 10f)
    var cooldownSeconds: Int = 0

    // The editor reads and writes RandomMedia directly; this field only gives it a slot.
    @Suppress("unused")
    @JvmField
    @Expose(serialize = false, deserialize = false)
    @ConfigOption(name = "Clips", desc = "Audio and video files that play at random.")
    @ConfigEditorRandomClips
    var clipList: String = ""
}
