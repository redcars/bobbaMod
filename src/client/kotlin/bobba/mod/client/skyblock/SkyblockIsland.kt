package bobba.mod.client.skyblock

/**
 * The SkyBlock islands/instances that auto-swap can react to.
 *
 * Declaration order is also match priority: [SkyblockLocation] returns the first entry whose
 * keyword appears in the detected area text, so entries whose keywords are substrings of another
 * ("Hub" is a substring of "Dungeon Hub") MUST come after the more specific entry.
 *
 * [keywords] are matched case-insensitively against the tab-list "Area:"/"Dungeon:" line, with the
 * scoreboard sidebar as a fallback. They intentionally err toward distinctive words so a keyword
 * can't leak into an unrelated area's text.
 */
enum class SkyblockIsland(
    val id: String,
    val displayName: String,
    val keywords: List<String>,
    /** True only for instances the scoreboard sidebar reliably names, so scoreboard-based matching
     * is restricted to them (see [matchScoreboard]) to avoid broad-keyword false positives. */
    val scoreboardMatchable: Boolean = false,
) {
    // Instances first — most specific, and the ones custom binds matter most for.
    KUUDRA("kuudra", "Kuudra", listOf("Kuudra"), scoreboardMatchable = true),
    CATACOMBS("catacombs", "The Catacombs", listOf("Catacombs"), scoreboardMatchable = true),
    DUNGEON_HUB("dungeon_hub", "Dungeon Hub", listOf("Dungeon Hub")),
    CRYSTAL_HOLLOWS("crystal_hollows", "Crystal Hollows", listOf("Crystal Hollows"), scoreboardMatchable = true),
    DWARVEN_MINES("dwarven_mines", "Dwarven Mines", listOf("Dwarven Mines"), scoreboardMatchable = true),
    DEEP_CAVERNS("deep_caverns", "Deep Caverns", listOf("Deep Caverns"), scoreboardMatchable = true),
    GOLD_MINE("gold_mine", "Gold Mine", listOf("Gold Mine")),
    CRIMSON_ISLE("crimson_isle", "Crimson Isle", listOf("Crimson Isle"), scoreboardMatchable = true),
    THE_RIFT("the_rift", "The Rift", listOf("The Rift")),
    THE_END("the_end", "The End", listOf("The End")),
    SPIDERS_DEN("spiders_den", "Spider's Den", listOf("Spider's Den", "Spider’s Den", "Spiders Den")),
    JERRYS_WORKSHOP("jerrys_workshop", "Jerry's Workshop", listOf("Jerry's Workshop", "Jerry’s Workshop", "Winter Island")),
    BACKWATER_BAYOU("backwater_bayou", "Backwater Bayou", listOf("Backwater Bayou")),
    THE_FARMING_ISLANDS("farming_islands", "The Farming Islands", listOf("Farming Island")),
    THE_PARK("the_park", "The Park", listOf("The Park")),
    GARDEN("garden", "The Garden", listOf("Garden")),
    PRIVATE_ISLAND("private_island", "Private Island", listOf("Private Island", "Your Island")),
    // Generic hub last so "Hub" doesn't shadow "Dungeon Hub".
    HUB("hub", "Hub", listOf("Hub")),
    ;

    companion object {
        fun byId(id: String): SkyblockIsland? = entries.firstOrNull { it.id == id }

        /** First island whose keyword appears (case-insensitively) in [areaText], or null. */
        fun match(areaText: String): SkyblockIsland? = matchIn(entries.asIterable(), areaText)

        /**
         * Like [match] but only considers [scoreboardMatchable] instances, so the broad scoreboard
         * sidebar scan can't false-positive on generic keywords ("Hub", "The End", "Garden").
         */
        fun matchScoreboard(text: String): SkyblockIsland? =
            matchIn(entries.filter { it.scoreboardMatchable }, text)

        private fun matchIn(candidates: Iterable<SkyblockIsland>, text: String): SkyblockIsland? {
            val hay = text.lowercase()
            return candidates.firstOrNull { island ->
                island.keywords.any { hay.contains(it.lowercase()) }
            }
        }
    }
}
