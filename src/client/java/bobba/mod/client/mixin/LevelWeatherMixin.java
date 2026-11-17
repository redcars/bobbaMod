package bobba.mod.client.mixin;

import bobba.mod.client.world.WorldOverrides;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Rain, sky colour, particles and sounds all read these levels. */
@Mixin(Level.class)
public class LevelWeatherMixin {
	// Level is shared with the singleplayer server, whose weather must stay real.
	@ModifyReturnValue(method = "getRainLevel", at = @At("RETURN"))
	private float bobbamod$overrideRain(float level) {
		return (Object) this instanceof ClientLevel ? WorldOverrides.rainLevel(level) : level;
	}

	@ModifyReturnValue(method = "getThunderLevel", at = @At("RETURN"))
	private float bobbamod$overrideThunder(float level) {
		return (Object) this instanceof ClientLevel ? WorldOverrides.thunderLevel(level) : level;
	}
}
