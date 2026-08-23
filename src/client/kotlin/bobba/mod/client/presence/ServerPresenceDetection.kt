package bobba.mod.client.presence

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.notify.Notifier
import bobba.mod.client.watchlist.Watchlist
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import java.util.UUID

object ServerPresenceDetection {
    private const val INITIAL_DELAY_TICKS = 40   // 2s after join
    private const val RESCAN_INTERVAL_TICKS = 100 // every 5s

    private val warnedThisSession = mutableSetOf<UUID>()
    private var ticksUntilScan = -1

    fun init() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            warnedThisSession.clear()
            ticksUntilScan = INITIAL_DELAY_TICKS
        }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            warnedThisSession.clear()
            ticksUntilScan = -1
        }
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (ticksUntilScan < 0) return@register
            ticksUntilScan--
            if (ticksUntilScan <= 0) {
                ticksUntilScan = RESCAN_INTERVAL_TICKS
                scan()
            }
        }
    }

    private fun scan() {
        if (!ConfigManager.instance.watchlist.warnOnServerPresence) return
        val mc = Minecraft.getInstance()
        val connection = mc.connection ?: return
        val selfUuid = mc.player?.uuid
        connection.onlinePlayers.forEach { info ->
            val uuid = info.profile.id
            if (uuid == selfUuid) return@forEach
            handlePlayerSeen(uuid, info.profile.name)
        }
    }

    private fun handlePlayerSeen(uuid: UUID, name: String) {
        if (uuid in warnedThisSession) return
        val entry = Watchlist.getByUuid(uuid) ?: Watchlist.getByIgn(name) ?: return
        warnedThisSession.add(uuid)
        Notifier.warn("Watchlisted player on this server: ${entry.ign}", entry.note)
    }

    fun simulateSeen(ign: String) {
        if (!ConfigManager.instance.watchlist.warnOnServerPresence) {
            Notifier.warn("(test) warnOnServerPresence is disabled — real warning would not fire.")
            return
        }
        if (!Watchlist.contains(ign)) {
            Notifier.warn("(test) $ign is not on the watchlist — real warning would not fire.")
            return
        }
        Notifier.warn("Watchlisted player on this server: $ign")
    }
}
