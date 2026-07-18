package bobba.mod.client.party

import bobba.mod.client.config.AutoKickFilters
import bobba.mod.client.config.ConfigManager
import bobba.mod.client.hypixel.HypixelRank
import bobba.mod.client.notify.Notifier
import bobba.mod.client.watchlist.Watchlist
import java.util.Optional
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
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

    /** A stretch of message text with one effective color, after resolving styles and legacy § codes. */
    private data class TextRun(val text: String, val rgb: Int?)

    fun init() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) handleMessage(message)
        }
    }

    fun handleMessage(message: Component) {
        val runs = flatten(message)
        detect(runs.joinToString("") { it.text }, runs)
    }

    /** Plain-text entry point used by /bobbatestparty; carries no color information. */
    fun handleMessage(plainText: String) {
        detect(plainText, emptyList())
    }

    private fun detect(plain: String, runs: List<TextRun>) {
        val trimmed = plain.trim()
        val match = partyJoinRegex.matchEntire(trimmed)
            ?: dungeonJoinRegex.matchEntire(trimmed)
            ?: return
        val prefix = match.groupValues[1].ifEmpty { null }
        val ign = match.groupValues[2]
        val parsedRank = prefix?.let { HypixelRank.fromPrefix(it) }

        // Party Finder joins carry no [RANK] text — the name's color is the only rank signal.
        val colorTier = if (parsedRank == null) {
            val leading = plain.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            val ignStart = leading + (match.groups[2]?.range?.first ?: 0)
            colorAt(runs, ignStart)?.let { rankTierFromColor(it) }
        } else {
            null
        }

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
            val filters = ConfigManager.instance.party.autoKickFilters
            val exactRank = parsedRank ?: watchlistEntry?.rank
            val rankAllows = when {
                exactRank != null -> shouldKickRank(exactRank, filters)
                colorTier != null -> shouldKickColorTier(colorTier, filters)
                else -> filters.kickUnranked
            }
            if (rankAllows || isWatchlisted) {
                suggestKick(ign, exactRank ?: colorTier ?: HypixelRank.NONE)
            }
        }
    }

    /**
     * Flattens a chat component into colored runs. Hypixel messages carry color either as
     * component styles or as legacy § codes embedded in the text, depending on how the server
     * serialized them; both are resolved here, with § codes taking precedence when present.
     */
    private fun flatten(message: Component): List<TextRun> {
        val runs = mutableListOf<TextRun>()
        var legacyColor: ChatFormatting? = null
        message.visit(FormattedText.StyledContentConsumer<Unit> { style, text ->
            val styleRgb = style.color?.value
            val segment = StringBuilder()
            fun flush() {
                if (segment.isNotEmpty()) {
                    runs.add(TextRun(segment.toString(), legacyColor?.color ?: styleRgb))
                    segment.clear()
                }
            }
            var i = 0
            while (i < text.length) {
                val c = text[i]
                if (c == '§' && i + 1 < text.length) {
                    flush()
                    val fmt = ChatFormatting.getByCode(text[i + 1])
                    if (fmt != null) {
                        if (fmt.isColor) legacyColor = fmt
                        else if (fmt == ChatFormatting.RESET) legacyColor = null
                    }
                    i += 2
                } else {
                    segment.append(c)
                    i++
                }
            }
            flush()
            Optional.empty()
        }, Style.EMPTY)
        return runs
    }

    private fun colorAt(runs: List<TextRun>, index: Int): Int? {
        var pos = 0
        for (run in runs) {
            val end = pos + run.text.length
            if (index < end) return run.rgb
            pos = end
        }
        return null
    }

    /**
     * Maps a name color to the rank tier it implies. Colors only narrow to a tier: green is
     * VIP or VIP+, aqua is MVP, MVP+, or an MVP++ with the aqua tag perk. Staff/YouTube colors
     * map to ranks that are never kick-suggested.
     */
    private fun rankTierFromColor(rgb: Int): HypixelRank? = when (rgb) {
        ChatFormatting.GREEN.color -> HypixelRank.VIP
        ChatFormatting.AQUA.color -> HypixelRank.MVP
        ChatFormatting.GOLD.color -> HypixelRank.MVP_PLUS_PLUS
        ChatFormatting.GRAY.color, ChatFormatting.WHITE.color -> HypixelRank.NONE
        ChatFormatting.RED.color -> HypixelRank.YOUTUBE
        ChatFormatting.BLUE.color -> HypixelRank.HELPER
        ChatFormatting.DARK_GREEN.color -> HypixelRank.MODERATOR
        else -> null
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

    /**
     * Filter check for color-derived ranks: since the color only identifies a tier, the tier
     * matches when any filter inside it is enabled.
     */
    private fun shouldKickColorTier(tier: HypixelRank, filters: AutoKickFilters): Boolean = when (tier) {
        HypixelRank.NONE -> filters.kickUnranked
        HypixelRank.VIP, HypixelRank.VIP_PLUS -> filters.kickVip || filters.kickVipPlus
        HypixelRank.MVP, HypixelRank.MVP_PLUS -> filters.kickMvp || filters.kickMvpPlus
        HypixelRank.MVP_PLUS_PLUS -> filters.kickMvpPlusPlus
        else -> false
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
            mc.gui.chat.addClientSystemMessage(
                Component.literal("[BobbaMod] ").withStyle(ChatFormatting.GOLD)
                    .append(nameComponent)
                    .append(Component.literal(" joined — ").withStyle(ChatFormatting.YELLOW))
                    .append(clickable)
            )
        }
    }
}
