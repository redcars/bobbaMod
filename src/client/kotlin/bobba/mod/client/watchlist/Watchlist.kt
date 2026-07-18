package bobba.mod.client.watchlist

import bobba.mod.client.hypixel.HypixelRank
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object Watchlist {
    private val byIgn = ConcurrentHashMap<String, WatchlistEntry>()

    val entries: Collection<WatchlistEntry>
        get() = byIgn.values

    fun add(entry: WatchlistEntry): Boolean {
        val key = entry.ign.lowercase()
        if (byIgn.putIfAbsent(key, entry) != null) return false
        save()
        return true
    }

    fun remove(ign: String): WatchlistEntry? {
        val removed = byIgn.remove(ign.lowercase()) ?: return null
        save()
        return removed
    }

    fun contains(ign: String): Boolean = byIgn.containsKey(ign.lowercase())

    fun getByIgn(ign: String): WatchlistEntry? = byIgn[ign.lowercase()]

    fun getByUuid(uuid: UUID): WatchlistEntry? =
        byIgn.values.firstOrNull { it.uuid == uuid }

    fun attachProfile(typedIgn: String, uuid: UUID, canonicalIgn: String) {
        val typedKey = typedIgn.lowercase()
        val existing = byIgn[typedKey] ?: getByUuid(uuid) ?: return
        val newKey = canonicalIgn.lowercase()
        val updated = existing.copy(ign = canonicalIgn, uuid = uuid)
        if (existing.ign.lowercase() != newKey) {
            byIgn.remove(existing.ign.lowercase())
        }
        byIgn[newKey] = updated
        save()
    }

    fun attachRank(uuid: UUID, rank: HypixelRank) {
        val existing = getByUuid(uuid) ?: return
        if (existing.rank == rank) return
        byIgn[existing.ign.lowercase()] = existing.copy(rank = rank)
        save()
    }

    fun attachRankByIgn(ign: String, rank: HypixelRank) {
        val existing = getByIgn(ign) ?: return
        if (existing.rank == rank) return
        byIgn[existing.ign.lowercase()] = existing.copy(rank = rank)
        save()
    }

    fun setNote(ign: String, note: String?) {
        val existing = getByIgn(ign) ?: return
        val normalized = note?.trim()?.takeIf { it.isNotEmpty() }
        if (existing.note == normalized) return
        byIgn[existing.ign.lowercase()] = existing.copy(note = normalized)
        save()
    }

    fun renameByUuid(uuid: UUID, newIgn: String): Boolean {
        val existing = getByUuid(uuid) ?: return false
        if (existing.ign.equals(newIgn, ignoreCase = true)) return false
        byIgn.remove(existing.ign.lowercase())
        byIgn[newIgn.lowercase()] = existing.copy(ign = newIgn)
        save()
        return true
    }

    fun load() {
        byIgn.clear()
        WatchlistStorage.load().forEach { byIgn[it.ign.lowercase()] = it }
    }

    fun save() {
        WatchlistStorage.save(byIgn.values)
    }
}
