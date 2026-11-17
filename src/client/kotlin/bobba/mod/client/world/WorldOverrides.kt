package bobba.mod.client.world

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.config.WorldCategory

/** Client-side time and weather the player picked in the World tab. */
object WorldOverrides {
    private const val DAY_LENGTH = 24000L

    private val config: WorldCategory get() = ConfigManager.instance.world

    /** Keeps the day count, so the moon phase still follows the server. */
    @JvmStatic
    fun overworldTicks(serverTicks: Long): Long {
        val c = config
        if (!c.overrideTime) return serverTicks
        return Math.floorDiv(serverTicks, DAY_LENGTH) * DAY_LENGTH + c.time.toLong().coerceIn(0L, DAY_LENGTH - 1)
    }

    @JvmStatic
    fun rainLevel(serverLevel: Float): Float {
        val c = config
        if (!c.overrideWeather) return serverLevel
        return if (c.weather == WorldCategory.Weather.CLEAR) 0f else 1f
    }

    @JvmStatic
    fun thunderLevel(serverLevel: Float): Float {
        val c = config
        if (!c.overrideWeather) return serverLevel
        return if (c.weather == WorldCategory.Weather.THUNDER) 1f else 0f
    }
}
