package bobba.mod.client.skyblock

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

/**
 * Detects the SkyBlock island/instance the player is currently on.
 *
 * The island identity comes from the tab-list "Area:" / "Dungeon:" line, which Hypixel keeps in
 * sync with the actual instance (the scoreboard's ⏣ line only names the local sub-zone, e.g.
 * "Village", so it can't identify the island). The scoreboard sidebar is scanned as a fallback,
 * which mainly rescues instance detection (Kuudra/Catacombs) if the tab line is missing.
 *
 * Detection is throttled and change-driven: [onChange] listeners fire only when the island
 * actually changes (including to/from null when leaving SkyBlock).
 */
object SkyblockLocation {
    private const val SCAN_INTERVAL_TICKS = 20 // ~1s

    private val codeRegex = Regex("§.")
    private val areaRegex = Regex("""(?:Area|Dungeon):\s*(.+)""")

    var current: SkyblockIsland? = null
        private set

    private val listeners = mutableListOf<(SkyblockIsland?) -> Unit>()
    private var ticks = 0

    /** Registers a listener invoked whenever the detected island changes. */
    fun onChange(listener: (SkyblockIsland?) -> Unit) {
        listeners += listener
    }

    fun init() {
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> update(null) }
        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            if (mc.player == null) {
                update(null)
                return@register
            }
            if (++ticks < SCAN_INTERVAL_TICKS) return@register
            ticks = 0
            update(detect(mc))
        }
    }

    private fun update(next: SkyblockIsland?) {
        if (next == current) return
        current = next
        listeners.toList().forEach { it(next) }
    }

    private fun detect(mc: Minecraft): SkyblockIsland? {
        val area = readAreaFromTabList(mc) ?: readScoreboardText(mc) ?: return null
        return SkyblockIsland.match(area)
    }

    /** Reads the raw area text (e.g. "Hub", "Dungeon Hub") from the tab-list "Area:"/"Dungeon:" line. */
    fun readAreaFromTabList(mc: Minecraft): String? {
        val connection = mc.connection ?: return null
        for (info in connection.onlinePlayers) {
            val raw = info.tabListDisplayName?.string ?: continue
            val match = areaRegex.find(stripCodes(raw)) ?: continue
            return match.groupValues[1].trim()
        }
        return null
    }

    /** Concatenates the visible scoreboard sidebar lines for keyword matching. */
    fun readScoreboardText(mc: Minecraft): String? {
        val scoreboard = mc.level?.scoreboard ?: return null
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return null
        val text = buildString {
            scoreboard.listPlayerScores(objective).forEach { entry ->
                if (entry.isHidden) return@forEach
                val team = scoreboard.getPlayersTeam(entry.owner)
                append(stripCodes(PlayerTeam.formatNameForTeam(team, entry.ownerName()).string))
                append('\n')
            }
        }
        return text.ifBlank { null }
    }

    private fun stripCodes(s: String): String = codeRegex.replace(s, "")
}
