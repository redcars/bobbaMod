package bobba.mod.client.mixin;

import bobba.mod.client.players.PlayerLookState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements PlayerLookState {
	@Unique
	private float bobbamod$modelAlpha = 1f;
	@Unique
	private float bobbamod$armorAlpha = 1f;

	@Override
	public float bobbamod$modelAlpha() {
		return bobbamod$modelAlpha;
	}

	@Override
	public float bobbamod$armorAlpha() {
		return bobbamod$armorAlpha;
	}

	@Override
	public void bobbamod$setAlphas(float model, float armor) {
		bobbamod$modelAlpha = model;
		bobbamod$armorAlpha = armor;
	}
}
