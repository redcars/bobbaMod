package bobba.mod.client.players

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.config.PlayerLook
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.world.entity.Avatar

/** Applies the Players tab's size and opacity to a player's render state. Render thread only. */
object PlayerLooks {
    // Hypixel gives its NPCs version-2 UUIDs; real accounts are version 4.
    private const val NPC_UUID_VERSION = 2

    @JvmStatic
    fun apply(entity: Avatar, state: AvatarRenderState) {
        val look = lookFor(entity)
        val alphas = state as PlayerLookState
        if (look == null) {
            alphas.`bobbamod$setAlphas`(1f, 1f)
            return
        }
        val size = look.size.coerceIn(0.1f, 3f)
        if (size != 1f) {
            state.scale *= size
            // The name tag would otherwise float at normal height above a resized body.
            state.nameTagAttachment = state.nameTagAttachment?.scale(size.toDouble())
        }
        alphas.`bobbamod$setAlphas`(look.modelOpacity.coerceIn(0, 100) / 100f, look.armorOpacity.coerceIn(0, 100) / 100f)
    }

    private fun lookFor(entity: Avatar): PlayerLook? {
        val config = ConfigManager.instance.players
        // Mannequins are avatars too, but they aren't players.
        if (!config.enabled || entity !is AbstractClientPlayer) return null
        if (entity === Minecraft.getInstance().player) return config.self
        if (!config.includeNpcs && entity.uuid.version() == NPC_UUID_VERSION) return null
        return config.others
    }
}
