package bobba.mod.client.kuudra

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.skyblock.SkyblockLocation
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

/**
 * In Kuudra's Hollow, hides the lair's walls, roof and surroundings ([KuudraLairTemplate]) so the
 * sky shows through. Purely visual: chunk meshes skip the blocks (RenderSectionRegionMixin), but
 * they still exist for collision and the server.
 */
object KuudraLair {
    // Read by chunk-meshing worker threads.
    @Volatile
    private var active = false

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { refresh() }
    }

    /** True if [state] at [pos] should not be drawn. Called off the render thread. */
    @JvmStatic
    fun isHidden(pos: BlockPos, state: BlockState): Boolean =
        active && KuudraLairTemplate.isHidden(pos.x, pos.y, pos.z, state)

    /**
     * True where the lair is hidden, whatever the block. Faces next to such spots read their light
     * from them, and a hidden solid block has light 0, so they'd render black; treat them as open sky.
     */
    @JvmStatic
    fun isLitAsSky(pos: BlockPos): Boolean = active && !KuudraLairTemplate.isKept(pos.x, pos.y, pos.z)

    private fun refresh() {
        val next = ConfigManager.instance.kuudra.hideLair && SkyblockLocation.inKuudraLair
        if (next == active) return
        active = next
        // Hiding covers everything outside the arena, so every loaded chunk can change (like F3+A).
        val mc = Minecraft.getInstance()
        if (mc.level != null) mc.levelExtractor.allChanged()
    }
}
