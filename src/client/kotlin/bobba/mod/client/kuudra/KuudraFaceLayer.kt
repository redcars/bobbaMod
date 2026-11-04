package bobba.mod.client.kuudra

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.monster.slime.MagmaCubeModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.SlimeRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture

/**
 * Draws the face image as one quad just in front of the magma cube's front side. The pose here is
 * model space in 1/16-block units: the body spans x -4..4, y 16..24 (y points down), and the face
 * is the z = -4 side.
 */
class KuudraFaceLayer(parent: RenderLayerParent<SlimeRenderState, MagmaCubeModel>) :
    RenderLayer<SlimeRenderState, MagmaCubeModel>(parent) {

    private companion object {
        const val LEFT = -4f / 16
        const val RIGHT = 4f / 16
        const val TOP = 16f / 16
        const val BOTTOM = 24f / 16

        // Slightly in front of the cube so the face texture can't z-fight through.
        const val FRONT = -4.05f / 16
    }

    override fun submit(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int,
        state: SlimeRenderState,
        yRot: Float,
        xRot: Float,
    ) {
        val image = KuudraFace.imageFor(state) ?: return
        val frame = image.frameAt(System.currentTimeMillis())
        val (u0, v0, u1, v1) = KuudraFace.squareCrop(image)

        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(frame)) { pose, consumer ->
            // Counter-clockwise as seen from in front; model -x is the viewer's left.
            fun vertex(x: Float, y: Float, u: Float, v: Float) {
                consumer.addVertex(pose, x, y, FRONT)
                    .setColor(-1)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, 0f, 0f, -1f)
            }
            vertex(LEFT, BOTTOM, u0, v1)
            vertex(RIGHT, BOTTOM, u1, v1)
            vertex(RIGHT, TOP, u1, v0)
            vertex(LEFT, TOP, u0, v0)
        }
    }
}
