package bobba.mod.client.keybinds

data class KeybindEntry(
    val keyCode: Int,
    val command: String,
    val enabled: Boolean? = true,
) {
    val isEnabled: Boolean get() = enabled ?: true
}
