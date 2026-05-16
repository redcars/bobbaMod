package bobba.mod.client.watchlist

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

    fun attachUuid(ign: String, uuid: UUID) {
        val key = ign.lowercase()
        val existing = byIgn[key] ?: return
        if (existing.uuid == uuid) return
        byIgn[key] = existing.copy(uuid = uuid)
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
