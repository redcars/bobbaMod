package bobba.mod.client.skyblock

import bobba.mod.client.keybinds.Keybinds
import bobba.mod.client.notify.Notifier
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

/**
 * Switches the active keybind preset to match the current SkyBlock island.
 *
 * When on an island mapped to a preset, that preset becomes active; when on an unmapped island (or
 * off SkyBlock), it falls back to the configured default preset. Only ever changes the active
 * preset while auto-swap is enabled, and never announces a no-op switch.
 */
object AutoPresetSwitcher {
    private var screenWasOpen = false

    fun init() {
        SkyblockLocation.onChange { island -> apply(island) }
        // Apply once a screen closes, so an island change that occurred while a GUI was open
        // (and was therefore skipped) takes effect without corrupting in-progress edits.
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            val screenOpen = mc.screen != null
            if (screenWasOpen && !screenOpen) applyCurrent()
            screenWasOpen = screenOpen
        }
    }

    /** Re-evaluates the correct preset for the current island; call after toggling auto-swap on. */
    fun applyCurrent() {
        apply(SkyblockLocation.current)
    }

    private fun apply(island: SkyblockIsland?) {
        if (!Keybinds.isAutoSwapEnabled()) return
        // Never change the active preset while a screen is open; the Keybind Editor writes to the
        // live active preset, so swapping under it would corrupt in-progress edits.
        if (Minecraft.getInstance().screen != null) return

        val target = island?.let { Keybinds.presetForIsland(it.id) } ?: Keybinds.defaultPreset()
        if (target == null || Keybinds.presets().none { it.name == target }) return
        if (Keybinds.activeName() == target) return

        Keybinds.setActive(target)
        val reason = island?.displayName ?: "unmapped area"
        Notifier.info("Keybind preset → $target ($reason)")
    }
}
