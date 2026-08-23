package bobba.mod.client.keybinds

data class KeybindPreset(
    val name: String,
    val keybinds: MutableList<KeybindEntry> = mutableListOf(),
    /** Ids of the SkyBlock islands that auto-switch to this preset (see SkyblockIsland). */
    val islands: MutableSet<String> = mutableSetOf(),
)

data class KeybindsData(
    val active: String,
    val presets: MutableList<KeybindPreset>,
    /** Master toggle for automatic preset switching based on the current SkyBlock island. */
    var autoSwap: Boolean = false,
    /** Preset to fall back to when on an island that isn't mapped to any preset. */
    var defaultPreset: String? = null,
)
