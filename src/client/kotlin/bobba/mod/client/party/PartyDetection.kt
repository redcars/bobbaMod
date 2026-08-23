package bobba.mod.client.party

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.notify.Notifier
import bobba.mod.client.watchlist.Watchlist
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

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

        val entry = Watchlist.getByIgn(ign) ?: return

        if (ConfigManager.instance.watchlist.warnOnPartyJoin) {
            val noteSuffix = entry.note?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""
            Notifier.warn("Watchlisted player joined your party: $ign$noteSuffix")
        }

        if (ConfigManager.instance.watchlist.autoKickOnPartyJoin) {
            kick(ign)
        }
    }

    private fun kick(ign: String) {
        val mc = Minecraft.getInstance()
        mc.execute {
            val connection = mc.player?.connection ?: return@execute
            connection.sendCommand("party kick $ign")
            mc.gui.chat.addClientSystemMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Auto-kicked watchlisted player: $ign").withStyle(ChatFormatting.RED))
            )
        }
    }

    /** Dev hook: exercises the kick path directly, regardless of watchlist state. */
    fun simulateKick(ign: String) = kick(ign)
}
