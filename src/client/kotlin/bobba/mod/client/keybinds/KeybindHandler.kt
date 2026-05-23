package bobba.mod.client.keybinds

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

object KeybindHandler {
    private val pressed = mutableMapOf<Int, Boolean>()

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            if (mc.screen != null || mc.player == null) {
                if (pressed.isNotEmpty()) pressed.clear()
                return@register
            }

            val window = mc.window
            val entries = Keybinds.all()
            val seenKeys = mutableSetOf<Int>()
            entries.forEach { entry ->
                val code = entry.keyCode
                if (code <= 0) return@forEach
                seenKeys += code
                val isDown = InputConstants.isKeyDown(window, code)
                val wasDown = pressed[code] ?: false
                pressed[code] = isDown
                if (isDown && !wasDown && entry.isEnabled) {
                    fire(entry.command)
                }
            }
            pressed.keys.retainAll(seenKeys)
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
