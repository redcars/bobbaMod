package bobba.mod.client.keybinds

object Keybinds {
    private val entries = mutableListOf<KeybindEntry>()

    fun all(): List<KeybindEntry> = entries.toList()

    fun add(entry: KeybindEntry) {
        entries.add(entry)
        save()
    }

    fun removeAt(index: Int): KeybindEntry? {
        if (index !in entries.indices) return null
        val removed = entries.removeAt(index)
        save()
        return removed
    }

    fun removeByKeyCode(keyCode: Int): Int {
        val before = entries.size
        entries.removeAll { it.keyCode == keyCode }
        val removed = before - entries.size
        if (removed > 0) save()
        return removed
    }

    fun updateAt(index: Int, entry: KeybindEntry) {
        if (index !in entries.indices) return
        entries[index] = entry
        save()
    }

    fun load() {
        entries.clear()
        entries.addAll(KeybindsStorage.load())
    }

    fun save() {
        KeybindsStorage.save(entries)
    }
}
