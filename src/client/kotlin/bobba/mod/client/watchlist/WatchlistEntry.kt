package bobba.mod.client.watchlist

import java.time.Instant
import java.util.UUID

data class WatchlistEntry(
    val ign: String,
    val uuid: UUID? = null,
    val addedAt: Instant = Instant.now(),
    val note: String? = null,
)
