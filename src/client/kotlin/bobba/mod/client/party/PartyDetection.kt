package bobba.mod.client.party

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.hypixel.HypixelRank
import bobba.mod.client.notify.Notifier
import bobba.mod.client.watchlist.Watchlist
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents

object PartyDetection {
    private val partyJoinRegex = Regex(
        """^(?:Party\s*>\s*)?(\[[^\]]+\] )?(\w{1,16}) (?:has )?joined (?:the |your )?party[.!]?$"""
    )

    private val dungeonJoinRegex = Regex(
        """^(\[[^\]]+\] )?(\w{1,16}) joined the dungeon group[.!]?$"""
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

        if (Watchlist.contains(ign) && prefix != null) {
            HypixelRank.fromPrefix(prefix)?.let { Watchlist.attachRankByIgn(ign, it) }
        }

        if (!ConfigManager.instance.watchlist.warnOnPartyJoin) return
        if (!Watchlist.contains(ign)) return
        Notifier.warn("Watchlisted player joined your party: $ign")
    }
}
