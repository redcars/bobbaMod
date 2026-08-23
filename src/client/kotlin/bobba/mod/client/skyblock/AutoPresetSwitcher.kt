package bobba.mod.client.skyblock

import bobba.mod.client.keybinds.Keybinds
import bobba.mod.client.notify.Notifier

/**
 * Switches the active keybind preset to match the current SkyBlock island.
 *
 * When on an island mapped to a preset, that preset becomes active; when on an unmapped island (or
 * off SkyBlock), it falls back to the configured default preset. Only ever changes the active
 * preset while auto-swap is enabled, and never announces a no-op switch.
 */
object AutoPresetSwitcher {
    fun init() {
        SkyblockLocation.onChange { island -> apply(island) }
    }

    /** Re-evaluates the correct preset for the current island; call after toggling auto-swap on. */
    fun applyCurrent() {
        apply(SkyblockLocation.current)
    }

    private fun apply(island: SkyblockIsland?) {
        if (!Keybinds.isAutoSwapEnabled()) return

        val target = island?.let { Keybinds.presetForIsland(it.id) } ?: Keybinds.defaultPreset()
        if (target == null || Keybinds.presets().none { it.name == target }) return
        if (Keybinds.activeName() == target) return

        Keybinds.setActive(target)
        val reason = island?.displayName ?: "unmapped area"
        Notifier.info("Keybind preset → $target ($reason)")
    }
}
