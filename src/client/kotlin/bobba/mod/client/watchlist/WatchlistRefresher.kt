package bobba.mod.client.watchlist

import bobba.mod.client.hypixel.HypixelApi
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

object WatchlistRefresher {
    private val logger = LoggerFactory.getLogger("bobbamod/refresh")

    fun init() {
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            val rawIp = Minecraft.getInstance().currentServer?.ip ?: return@register
            val host = rawIp.substringBefore(':').lowercase()
            if (!host.endsWith("hypixel.net")) return@register
            refreshAll()
        }
    }

    private fun refreshAll() {
        val entries = Watchlist.entries.toList()
        if (entries.isEmpty()) return
        logger.info("Refreshing {} watchlist entries on Hypixel join", entries.size)

        entries.forEach { entry ->
            val uuid = entry.uuid
            if (uuid != null) {
                MojangApi.resolveCurrentName(uuid).thenAccept { newName ->
                    if (newName != null && newName != entry.ign) {
                        Watchlist.renameByUuid(uuid, newName)
                    }
                }
                HypixelApi.resolveRank(uuid).thenAccept { rank ->
                    if (rank != null) Watchlist.attachRank(uuid, rank)
                }
            } else {
                MojangApi.resolveProfile(entry.ign).thenAccept { profile ->
                    if (profile == null) return@thenAccept
                    Watchlist.attachProfile(entry.ign, profile.uuid, profile.name)
                    HypixelApi.resolveRank(profile.uuid).thenAccept { rank ->
                        if (rank != null) Watchlist.attachRank(profile.uuid, rank)
                    }
                }
            }
        }
    }
}
