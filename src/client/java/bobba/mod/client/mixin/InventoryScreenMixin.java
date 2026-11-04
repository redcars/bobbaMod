package bobba.mod.client.mixin;

import bobba.mod.client.players.PlayerLookState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
	// Keeps the inventory preview fully visible; vanilla already resets its size here.
	@ModifyReturnValue(
		method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
		at = @At("RETURN")
	)
	private static EntityRenderState bobbamod$plainPreview(EntityRenderState state) {
		if (state instanceof PlayerLookState look) look.bobbamod$setAlphas(1f, 1f);
		return state;
	}
}
