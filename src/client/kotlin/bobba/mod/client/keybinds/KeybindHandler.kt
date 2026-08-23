package bobba.mod.client.keybinds

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

object KeybindHandler {
    // Ticks after a screen closes during which keybinds stay inert, so a keystroke
    // meant for the closing screen (e.g. the last letter typed in chat) can't leak
    // into a command. At 20 TPS this is roughly a quarter second.
    private const val GRACE_TICKS = 5

    // Previous tick's key states, keyed by key code. Rebuilt each tick from a fresh
    // snapshot so edge detection reads a stable "was down last tick" for every entry.
    private var pressed = mapOf<Int, Boolean>()
    private var graceRemaining = 0

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            if (mc.player == null) {
                if (pressed.isNotEmpty()) pressed = emptyMap()
                // Arm the grace window so a key still held when the world loads isn't
                // treated as a fresh press on the first in-world tick.
                graceRemaining = GRACE_TICKS
                return@register
            }

            // Keep tracking key state even while a screen is open so that a key
            // held across the screen-close transition (e.g. typing in chat) is
            // recorded as already-down and isn't treated as a fresh press.
            val screenOpen = mc.screen != null
            if (screenOpen) {
                graceRemaining = GRACE_TICKS
            } else if (graceRemaining > 0) {
                graceRemaining--
            }
            val allowFire = !screenOpen && graceRemaining == 0

            val window = mc.window
            // Sample each distinct key once and compare against the previous tick's
            // committed state — not a map mutated mid-loop — so several keybinds
            // sharing one key all see the same wasDown and each can fire.
            val nextPressed = mutableMapOf<Int, Boolean>()
            Keybinds.all().forEach { entry ->
                val code = entry.keyCode
                if (code <= 0) return@forEach
                val isDown = nextPressed.getOrPut(code) { InputConstants.isKeyDown(window, code) }
                val wasDown = pressed[code] ?: false
                if (allowFire && isDown && !wasDown && entry.isEnabled) {
                    fire(entry.command)
                }
            }
            pressed = nextPressed
        }
    }

    private fun fire(text: String) {
        val mc = Minecraft.getInstance()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        mc.execute {
            val connection = mc.player?.connection ?: return@execute
            if (trimmed.startsWith("/")) {
                connection.sendCommand(trimmed.removePrefix("/"))
            } else {
                connection.sendChat(trimmed)
            }
        }
    }
}
