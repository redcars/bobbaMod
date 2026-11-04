package bobba.mod.client.mixin;

import bobba.mod.client.players.PlayerLookState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The cape follows the model opacity, so a see-through player isn't left with a solid cape. */
@Mixin(CapeLayer.class)
public class CapeLayerMixin {
	@SuppressWarnings({"unchecked", "rawtypes"})
	@WrapOperation(
		method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
		)
	)
	private void bobbamod$fadeCape(
		SubmitNodeCollector collector, Model<?> model, Object state, PoseStack poseStack, RenderType renderType,
		int lightCoords, int overlayCoords, int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling,
		Operation<Void> original
	) {
		float alpha = PlayerLookState.modelAlpha(state);
		if (alpha >= 1f) {
			original.call(collector, model, state, poseStack, renderType, lightCoords, overlayCoords, outlineColor, crumbling);
			return;
		}
		if (alpha <= 0f) return;
		// The cape draws solid, which ignores alpha.
		RenderType translucent = RenderTypes.entityTranslucent(((AvatarRenderState) state).skin.cape().texturePath());
		collector.submitModel((Model) model, state, poseStack, translucent, lightCoords, overlayCoords,
			PlayerLookState.fade(-1, alpha), null, outlineColor, crumbling);
	}
}
