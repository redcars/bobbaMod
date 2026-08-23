package bobba.mod.client.keybinds

object Keybinds {
    private const val DEFAULT_PRESET_NAME = "Default"

    private var data: KeybindsData = freshData()

    fun load() {
        data = KeybindsStorage.load()
        if (data.presets.isEmpty()) {
            data = freshData()
        }
        if (data.presets.none { it.name == data.active }) {
            data = data.copy(active = data.presets.first().name)
            save()
        }
        if (data.defaultPreset != null && data.presets.none { it.name == data.defaultPreset }) {
            data.defaultPreset = null
            save()
        }
    }

    fun save() = KeybindsStorage.save(data)

    // --- Presets ---

    fun presets(): List<KeybindPreset> = data.presets.toList()

    fun activeName(): String = data.active

    private fun activePreset(): KeybindPreset = data.presets.first { it.name == data.active }

    fun setActive(name: String): Boolean {
        if (data.presets.none { it.name == name }) return false
        if (data.active == name) return true
        data = data.copy(active = name)
        save()
        return true
    }

    fun addPreset(name: String): Boolean {
        if (name.isBlank()) return false
        if (data.presets.any { it.name == name }) return false
        data.presets.add(KeybindPreset(name, mutableListOf()))
        save()
        return true
    }

    fun removePreset(name: String): Boolean {
        if (data.presets.size <= 1) return false
        val removed = data.presets.removeAll { it.name == name }
        if (!removed) return false
        if (data.active == name) {
            data = data.copy(active = data.presets.first().name)
        }
        if (data.defaultPreset == name) {
            data.defaultPreset = null
        }
        save()
        return true
    }

    // --- Auto-swap (island → preset) ---

    fun isAutoSwapEnabled(): Boolean = data.autoSwap

    fun setAutoSwap(enabled: Boolean) {
        if (data.autoSwap == enabled) return
        data.autoSwap = enabled
        save()
    }

    fun defaultPreset(): String? = data.defaultPreset

    fun setDefaultPreset(name: String?) {
        if (name != null && data.presets.none { it.name == name }) return
        if (data.defaultPreset == name) return
        data.defaultPreset = name
        save()
    }

    /** The preset an island is mapped to, or null if unmapped. */
    fun presetForIsland(islandId: String): String? =
        data.presets.firstOrNull { islandId in it.islands }?.name

    /** Maps an island to a preset, removing it from any other preset so each island maps to one. */
    fun assignIsland(islandId: String, presetName: String): Boolean {
        val target = data.presets.firstOrNull { it.name == presetName } ?: return false
        data.presets.forEach { if (it !== target) it.islands.remove(islandId) }
        target.islands.add(islandId)
        save()
        return true
    }

    fun unassignIsland(islandId: String) {
        var changed = false
        data.presets.forEach { if (it.islands.remove(islandId)) changed = true }
        if (changed) save()
    }

    // --- Keybinds (active preset) ---

    fun all(): List<KeybindEntry> = activePreset().keybinds.toList()

    fun add(entry: KeybindEntry) {
        activePreset().keybinds.add(entry)
        save()
    }

    fun removeAt(index: Int): KeybindEntry? {
        val list = activePreset().keybinds
        if (index !in list.indices) return null
        val removed = list.removeAt(index)
        save()
        return removed
    }

    fun removeByKeyCode(keyCode: Int): Int {
        val list = activePreset().keybinds
        val before = list.size
        list.removeAll { it.keyCode == keyCode }
        val removed = before - list.size
        if (removed > 0) save()
        return removed
    }

    fun updateAt(index: Int, entry: KeybindEntry) {
        val list = activePreset().keybinds
        if (index !in list.indices) return
        list[index] = entry
        save()
    }

    private fun freshData() = KeybindsData(
        DEFAULT_PRESET_NAME,
        mutableListOf(KeybindPreset(DEFAULT_PRESET_NAME, mutableListOf())),
    )
}
