package bobba.mod.client.kuudra

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * Which Kuudra's Hollow blocks stay visible when the lair is hidden. Coordinates were measured in a
 * downloaded copy of the lair (2026-11-04); the instance map is the same for every run.
 */
object KuudraLairTemplate {
    /** Inclusive block box. */
    class Box(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) {
        val minX = minOf(x1, x2)
        val minY = minOf(y1, y2)
        val minZ = minOf(z1, z2)
        val maxX = maxOf(x1, x2)
        val maxY = maxOf(y1, y2)
        val maxZ = maxOf(z1, z2)

        fun contains(x: Int, y: Int, z: Int) = x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    // The arena, open downwards: everything in its X/Z up to its top stays.
    const val MIN_X = -142
    // The island pokes out to -63 on its east side; the lair wall starts at -56.
    const val MAX_X = -63
    const val MIN_Z = -141
    const val MAX_Z = -63
    const val MAX_Y = 78

    // The lava sea around the arena stays, even outside the arena's X/Z.
    const val LAVA_MAX_Y = 74

    /** Further parts of the lair that stay, given as the two measured corners. */
    val KEEP = listOf(
        Box(-106, 82, -135, -98, 75, -179),
        Box(-107, 99, -176, -98, 74, -191),
        Box(-131, 12, -135, -185, 67, -193),
    )

    /** True inside the arena or a [KEEP] box, whatever the block. */
    fun isKept(x: Int, y: Int, z: Int): Boolean =
        (x in MIN_X..MAX_X && z in MIN_Z..MAX_Z && y <= MAX_Y) || KEEP.any { it.contains(x, y, z) }

    fun isHidden(x: Int, y: Int, z: Int, state: BlockState): Boolean {
        if (state.isAir) return false
        if (state.`is`(Blocks.LAVA) && y <= LAVA_MAX_Y) return false
        return !isKept(x, y, z)
    }
}
