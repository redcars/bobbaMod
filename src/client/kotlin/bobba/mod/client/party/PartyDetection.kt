package bobba.mod.client.party

import bobba.mod.client.config.AutoKickFilters
import bobba.mod.client.config.ConfigManager
import bobba.mod.client.hypixel.HypixelRank
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
        """^(?:Party\s*>\s*)?(\[[^\]]+] )?(\w{1,16}) (?:has )?joined (?:the |your )?party[.!]?$"""
    )

    @Suppress("RegExpRedundantEscape")
    private val dungeonJoinRegex = Regex(
        """^(?:Party Finder\s*>\s*)?(\[[^\]]+] )?(\w{1,16}) (?:has )?joined the dungeon group[.!]?(?:\s+\([^)]*\))?$"""
    )

    fun init() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) handleMessage(message.string)
        }
    }

    fun handleMessage(plainText: String) {
        val trimmed = plainText.trim()
        val match = partyJoinRegex.matchEntire(trimmed)
            ?: dungeonJoinRegex.matchEntire(trimmed)
            ?: return
        val prefix = match.groupValues[1].ifEmpty { null }
        val ign = match.groupValues[2]
        val parsedRank = prefix?.let { HypixelRank.fromPrefix(it) }

        val isWatchlisted = Watchlist.contains(ign)
        val watchlistEntry = if (isWatchlisted) Watchlist.getByIgn(ign) else null
        if (isWatchlisted && parsedRank != null) {
            Watchlist.attachRankByIgn(ign, parsedRank)
        }

        // Watchlist warning — only fires for watchlisted players.
        if (isWatchlisted && ConfigManager.instance.watchlist.warnOnPartyJoin) {
            val note = watchlistEntry?.note?.takeIf { it.isNotBlank() }
            val noteSuffix = note?.let { " — $it" } ?: ""
            Notifier.warn("Watchlisted player joined your party: $ign$noteSuffix")
        }

        // Kick suggestion — runs for any party / dungeon-group joiner whose rank matches the filters.
        // Watchlisted players bypass the rank filters entirely (still gated by the master toggle).
        // Dungeon-group joins are how party-finder additions surface; /party kick works for them too.
        if (ConfigManager.instance.party.autoKickFromParty) {
            val effectiveRank = parsedRank
                ?: watchlistEntry?.rank
                ?: HypixelRank.NONE
            val rankAllows = shouldKickRank(effectiveRank, ConfigManager.instance.party.autoKickFilters)
            if (rankAllows || isWatchlisted) {
                suggestKick(ign, effectiveRank)
            }
        }
    }

    private fun shouldKickRank(rank: HypixelRank, filters: AutoKickFilters): Boolean = when (rank) {
        HypixelRank.NONE -> filters.kickUnranked
        HypixelRank.VIP -> filters.kickVip
        HypixelRank.VIP_PLUS -> filters.kickVipPlus
        HypixelRank.MVP -> filters.kickMvp
        HypixelRank.MVP_PLUS -> filters.kickMvpPlus
        HypixelRank.MVP_PLUS_PLUS -> filters.kickMvpPlusPlus
        HypixelRank.YOUTUBE,
        HypixelRank.HELPER,
        HypixelRank.MODERATOR,
        HypixelRank.GAME_MASTER,
        HypixelRank.ADMIN,
        HypixelRank.OWNER -> false
    }

    private fun suggestKick(ign: String, rank: HypixelRank) {
        val mc = Minecraft.getInstance()
        mc.execute {
            val rankLabel = if (rank.prefix.isNotEmpty()) "${rank.prefix} " else ""
            val nameComponent = Component.literal("$rankLabel$ign").withStyle(rank.color)
            val clickable = Component.literal("[Click to kick]").withStyle(
                Style.EMPTY
                    .withColor(ChatFormatting.RED)
                    .withUnderlined(true)
                    .withClickEvent(ClickEvent.RunCommand("/party kick $ign"))
                    .withHoverEvent(HoverEvent.ShowText(Component.literal("Run /party kick $ign")))
            )
            mc.gui.chat.addMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(nameComponent)
                    .append(Component.literal(" joined — ").withStyle(ChatFormatting.YELLOW))
                    .append(clickable)
            )
        }
    }
}
