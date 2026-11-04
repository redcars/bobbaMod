package bobba.mod.client.kuudra

import bobba.mod.client.config.ConfigManager
import bobba.mod.client.media.LoadedImage
import bobba.mod.client.media.MediaLibrary
import bobba.mod.client.media.MediaTextures
import bobba.mod.client.skyblock.SkyblockLocation
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback
import net.minecraft.client.renderer.entity.MagmaCubeRenderer
import net.minecraft.client.renderer.entity.state.SlimeRenderState
import net.minecraft.world.entity.EntityTypes

/** Draws the user's image over the Kuudra boss's face. Render-thread only. */
object KuudraFace {
    // Hypixel's Kuudra is a size-30 magma cube; the other cubes in the lair are far smaller.
    private const val KUUDRA_SIZE = 30

    val imageName: String? get() = ConfigManager.instance.kuudra.faceImage

    fun init() {
        MediaLibrary.addReferences { listOfNotNull(imageName) }
        LivingEntityRenderLayerRegistrationCallback.EVENT.register { entityType, renderer, helper, _ ->
            if (entityType == EntityTypes.MAGMA_CUBE && renderer is MagmaCubeRenderer) {
                helper.register(KuudraFaceLayer(renderer))
            }
        }
    }

    /** The image to draw on [state], or null when this cube should look normal. */
    fun imageFor(state: SlimeRenderState): LoadedImage? {
        val kuudra = ConfigManager.instance.kuudra
        if (!kuudra.faceOverride || state.isInvisible || state.size < KUUDRA_SIZE) return null
        if (!SkyblockLocation.inKuudraLair) return null
        return MediaTextures.get(kuudra.faceImage ?: return null)
    }

    /** Normalised (u0, v0, u1, v1) of the centred square of [image], so it covers a square face unstretched. */
    fun squareCrop(image: LoadedImage): FloatArray {
        val aspect = image.width.toFloat() / image.height
        return if (aspect > 1f) {
            val span = 1f / aspect
            floatArrayOf((1f - span) / 2, 0f, (1f + span) / 2, 1f)
        } else {
            val span = aspect
            floatArrayOf(0f, (1f - span) / 2, 1f, (1f + span) / 2)
        }
    }

    fun setImage(name: String?) {
        val kuudra = ConfigManager.instance.kuudra
        val old = kuudra.faceImage
        kuudra.faceImage = name
        ConfigManager.save()
        if (old != null && old != name) MediaLibrary.deleteIfUnused(old)
    }
}
