package bobba.mod.client.keybinds

data class KeybindPreset(
    val name: String,
    val keybinds: MutableList<KeybindEntry> = mutableListOf(),
)

data class KeybindsData(
    val active: String,
    val presets: MutableList<KeybindPreset>,
)
