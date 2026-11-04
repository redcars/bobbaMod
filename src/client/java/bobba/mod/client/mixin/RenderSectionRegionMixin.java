package bobba.mod.client.mixin;

import bobba.mod.client.kuudra.KuudraLair;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Chunk meshing reads every block and light level through this region, so reporting air here skips
// the block and makes its neighbours draw the faces that touched it. ModifyReturnValue avoids
// allocating a callback object on this very hot path.
@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin implements BlockAndTintGetter {
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState bobbamod$hideLairBlocks(BlockState state, BlockPos pos) {
        return KuudraLair.isHidden(pos, state) ? Blocks.AIR.defaultBlockState() : state;
    }

    // Overrides the interface default, which the region doesn't implement itself.
    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        if (KuudraLair.isLitAsSky(pos)) return 15;
        return BlockAndTintGetter.super.getBrightness(layer, pos);
    }
}
