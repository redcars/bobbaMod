package bobba.mod.client.party

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.notify.Notifier
import bobba.mod.client.watchlist.Watchlist
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

object PartyDetection {
    // `\]` inside the character class is required by Java's regex grammar;
    // IntelliJ flags it as redundant anyway, so we suppress.
    @Suppress("RegExpRedundantEscape")
    private val partyJoinRegex = Regex(
        """^(?:Party\s*>\s*)?(?:\[[^\]]+] )?(\w{1,16}) (?:has )?joined (?:the |your )?party[.!]?$"""
    )

    @Suppress("RegExpRedundantEscape")
    private val dungeonJoinRegex = Regex(
        """^(?:Party Finder\s*>\s*)?(?:\[[^\]]+] )?(\w{1,16}) (?:has )?joined the dungeon group[.!]?(?:\s+\([^)]*\))?$"""
    )

    private val codeRegex = Regex("§.")

    fun init() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) handleMessage(message)
        }
    }

    fun handleMessage(message: Component) = handleMessage(message.string)

    /** Entry point for both live chat and /bobbatestparty; rank/color are intentionally ignored. */
    fun handleMessage(plainText: String) {
        detect(codeRegex.replace(plainText, "").trim())
    }

    private fun detect(trimmed: String) {
        val match = partyJoinRegex.matchEntire(trimmed)
            ?: dungeonJoinRegex.matchEntire(trimmed)
            ?: return
        val ign = match.groupValues[1]

        // Party Finder prints your own dungeon-group join line to the whole group;
        // never warn/kick/quick-kick yourself.
        val selfName = Minecraft.getInstance().player?.gameProfile?.name
        if (selfName != null && ign.equals(selfName, ignoreCase = true)) return

        val config = ConfigManager.instance
        val entry = Watchlist.getByIgn(ign)

        if (entry != null && config.watchlist.warnOnPartyJoin) {
            Notifier.warn("Watchlisted player joined your party: $ign", entry.note)
        }

        // Auto-kick already removes them, so skip the manual button in that case.
        if (entry != null && config.party.autoKickWatchlisted) {
            kick(ign)
            return
        }

        if (config.party.quickKickButton) {
            showQuickKickButton(ign)
        }
    }

    private fun kick(ign: String) {
        val mc = Minecraft.getInstance()
        mc.execute {
            val connection = mc.player?.connection ?: return@execute
            connection.sendCommand("party kick $ign")
            mc.gui.chat.addClientSystemMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Sent /party kick for watchlisted player: $ign").withStyle(ChatFormatting.RED))
            )
        }
    }

    /** Posts a clickable [Kick] button for any party joiner, watchlisted or not. */
    private fun showQuickKickButton(ign: String) {
        val mc = Minecraft.getInstance()
        mc.execute {
            val button = Component.literal("[Kick]").withStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.RED)
                    .withUnderlined(true)
                    .withClickEvent(ClickEvent.RunCommand("/party kick $ign"))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Run /party kick $ign")))
            )
            mc.gui.chat.addClientSystemMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("$ign joined — ").withStyle(ChatFormatting.YELLOW))
                    .append(button)
            )
        }
    }

    /** Dev hook: exercises the kick path directly, regardless of watchlist state. */
    fun simulateKick(ign: String) = kick(ign)
}
