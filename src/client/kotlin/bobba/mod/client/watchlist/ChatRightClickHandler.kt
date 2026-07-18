package bobba.mod.client.watchlist

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.watchlist.gui.ChatActionsScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

object ChatRightClickHandler {
    @JvmStatic
    fun handle(mouseX: Double, mouseY: Double): Boolean {
        if (!ConfigManager.instance.watchlist.rightClickContextMenu) return false

        val mc = Minecraft.getInstance()
        val font = mc.font
        val finder = ActiveTextCollector.ClickableStyleFinder(font, mouseX.toInt(), mouseY.toInt())
        mc.gui.chat.captureClickableText(finder, mouseX.toInt(), mouseY.toInt(), false)
        val style = finder.result() ?: return false
        val ign = extractIgn(style) ?: return false

        if (Watchlist.contains(ign)) {
            mc.gui.chat.addMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("$ign is already on your watchlist.").withStyle(ChatFormatting.YELLOW))
            )
            return true
        }

        mc.setScreen(ChatActionsScreen(mc.screen, ign))
        return true
    }

    private fun extractIgn(style: Style): String? {
        val click = style.clickEvent ?: return null
        val command = (click as? ClickEvent.SuggestCommand)?.command() ?: return null
        val parts = command.trim().split(Regex("\\s+"))
        if (parts.size < 2) return null
        val verb = parts[0].lowercase()
        if (verb !in PRIVATE_MSG_COMMANDS) return null
        val candidate = parts[1]
        return candidate.takeIf { it.matches(IGN_REGEX) }
    }

    private val PRIVATE_MSG_COMMANDS = setOf(
        "/msg", "/w", "/tell", "/whisper", "/r"
    )

    private val IGN_REGEX = Regex("^\\w{2,16}$")
}
