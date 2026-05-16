package bobba.mod.client.watchlist

import bobba.mod.client.hypixel.HypixelRank
import java.time.Instant
import java.util.UUID

data class WatchlistEntry(
    val ign: String,
    val uuid: UUID? = null,
    val rank: HypixelRank? = null,
    val addedAt: Instant = Instant.now(),
    val note: String? = null,
)
