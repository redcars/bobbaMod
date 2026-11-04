package bobba.mod.client.mixin;

import bobba.mod.client.players.PlayerLookState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fades armor and elytra drawn on a player whose armor opacity is below 100%. */
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
	// The overload that draws; the shorter one only delegates to it.
	private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

	// Armor normally draws cutout, which ignores alpha.
	@WrapOperation(
		method = RENDER_LAYERS,
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
		)
	)
	private RenderType bobbamod$translucentArmor(Identifier texture, Operation<RenderType> original, @Local(argsOnly = true) Object state) {
		return PlayerLookState.armorAlpha(state) < 1f ? RenderTypes.armorTranslucent(texture) : original.call(texture);
	}

	@WrapOperation(
		method = RENDER_LAYERS,
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
		)
	)
	private RenderType bobbamod$translucentTrim(boolean decal, Operation<RenderType> original, @Local(argsOnly = true) Object state) {
		return PlayerLookState.armorAlpha(state) < 1f ? RenderTypes.armorTranslucent(Sheets.ARMOR_TRIMS_SHEET) : original.call(decal);
	}

	@WrapOperation(
		method = RENDER_LAYERS,
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
		)
	)
	private void bobbamod$fadeArmor(
		OrderedSubmitNodeCollector collector, Model<?> model, Object state, PoseStack poseStack, RenderType renderType,
		int lightCoords, int overlayCoords, int color, TextureAtlasSprite sprite, int outlineColor,
		ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original
	) {
		float alpha = PlayerLookState.armorAlpha(state);
		if (alpha <= 0f) return;
		if (alpha < 1f) color = PlayerLookState.fade(color, alpha);
		original.call(collector, model, state, poseStack, renderType, lightCoords, overlayCoords, color, sprite, outlineColor, crumbling);
	}
}
