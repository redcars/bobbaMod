package bobba.mod.client.config

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WorldCategory {
    @JvmField
    @Expose
    @ConfigOption(name = "Override time", desc = "Show the time of day below instead of the server's. Only you see it.")
    @ConfigEditorBoolean
    var overrideTime: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Time of day", desc = "In ticks: 0 = sunrise, 6000 = noon, 12000 = sunset, 18000 = midnight.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 23900f, minStep = 100f)
    var time: Int = 6000

    @JvmField
    @Expose
    @ConfigOption(name = "Override weather", desc = "Show the weather below instead of the server's. Only you see it.")
    @ConfigEditorBoolean
    var overrideWeather: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Weather", desc = "Rain shows as snow in cold biomes and as nothing in deserts, as in normal Minecraft.")
    @ConfigEditorDropdown
    var weather: Weather = Weather.CLEAR

    enum class Weather(private val label: String) {
        CLEAR("Clear"),
        RAIN("Rain"),
        THUNDER("Thunderstorm");

        override fun toString() = label
    }
}
