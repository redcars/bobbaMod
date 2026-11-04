package bobba.mod.client.mixin;

import bobba.mod.client.players.PlayerLookState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
	// The player model already draws with a translucent render type, so only its alpha needs changing.
	@WrapOperation(
		method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
		)
	)
	private void bobbamod$fadePlayerModel(
		SubmitNodeCollector collector, Model<?> model, Object state, PoseStack poseStack, RenderType renderType,
		int lightCoords, int overlayCoords, int color, TextureAtlasSprite sprite, int outlineColor,
		ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original
	) {
		float alpha = PlayerLookState.modelAlpha(state);
		if (alpha <= 0f) return;
		if (alpha < 1f) color = PlayerLookState.fade(color, alpha);
		original.call(collector, model, state, poseStack, renderType, lightCoords, overlayCoords, color, sprite, outlineColor, crumbling);
	}
}
