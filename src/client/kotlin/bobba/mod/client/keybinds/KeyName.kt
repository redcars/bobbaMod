package bobba.mod.client.keybinds

import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

object KeyName {
    fun displayName(keyCode: Int): String {
        if (keyCode <= 0) return "Unbound"
        return try {
            InputConstants.Type.KEYSYM.getOrCreate(keyCode).displayName.string
        } catch (e: Exception) {
            "Key($keyCode)"
        }
    }

    fun parse(input: String): Int? {
        val s = input.trim().lowercase()
        if (s.isEmpty()) return null

        if (s.length == 1) {
            val c = s[0]
            if (c in 'a'..'z') return GLFW.GLFW_KEY_A + (c - 'a')
            if (c in '0'..'9') return GLFW.GLFW_KEY_0 + (c - '0')
        }
        if (s.matches(Regex("f\\d+"))) {
            val n = s.substring(1).toIntOrNull() ?: return null
            if (n in 1..25) return GLFW.GLFW_KEY_F1 + n - 1
        }
        return when (s) {
            "space" -> GLFW.GLFW_KEY_SPACE
            "enter", "return" -> GLFW.GLFW_KEY_ENTER
            "tab" -> GLFW.GLFW_KEY_TAB
            "shift", "lshift" -> GLFW.GLFW_KEY_LEFT_SHIFT
            "rshift" -> GLFW.GLFW_KEY_RIGHT_SHIFT
            "ctrl", "lctrl", "control" -> GLFW.GLFW_KEY_LEFT_CONTROL
            "rctrl" -> GLFW.GLFW_KEY_RIGHT_CONTROL
            "alt", "lalt" -> GLFW.GLFW_KEY_LEFT_ALT
            "ralt" -> GLFW.GLFW_KEY_RIGHT_ALT
            "esc", "escape" -> GLFW.GLFW_KEY_ESCAPE
            "backspace" -> GLFW.GLFW_KEY_BACKSPACE
            "delete", "del" -> GLFW.GLFW_KEY_DELETE
            "up" -> GLFW.GLFW_KEY_UP
            "down" -> GLFW.GLFW_KEY_DOWN
            "left" -> GLFW.GLFW_KEY_LEFT
            "right" -> GLFW.GLFW_KEY_RIGHT
            "home" -> GLFW.GLFW_KEY_HOME
            "end" -> GLFW.GLFW_KEY_END
            "pageup", "pgup" -> GLFW.GLFW_KEY_PAGE_UP
            "pagedown", "pgdn" -> GLFW.GLFW_KEY_PAGE_DOWN
            else -> null
        }
    }
}
