package bobba.mod.client.mixin;

import bobba.mod.client.world.WorldOverrides;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Sky, light and sun position all read the overworld clock through here. */
@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {
	@ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"))
	private long bobbamod$overrideTime(long ticks, Holder<WorldClock> definition) {
		return definition.is(WorldClocks.OVERWORLD) ? WorldOverrides.overworldTicks(ticks) : ticks;
	}
}
