package bobba.mod.client.mixin;

import bobba.mod.client.scoreboard.CustomScoreboard;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudScoreboardMixin {
	// Replacing only the drawing keeps vanilla's choice of objective and its place in the HUD order.
	@Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
	private void bobbamod$drawCustomScoreboard(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
		if (!CustomScoreboard.getActive()) return;
		CustomScoreboard.render(graphics, objective);
		ci.cancel();
	}
}
