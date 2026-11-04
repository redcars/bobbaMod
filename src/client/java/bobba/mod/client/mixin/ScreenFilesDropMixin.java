package bobba.mod.client.mixin;

import bobba.mod.client.media.MediaDropTarget;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.List;

// Screens that handle drops themselves (e.g. the resource pack screen) override this and never reach here.
@Mixin(Screen.class)
public class ScreenFilesDropMixin {
    @Inject(method = "onFilesDrop", at = @At("HEAD"), cancellable = true)
    private void bobbamod$onFilesDrop(List<Path> paths, CallbackInfo ci) {
        if (MediaDropTarget.INSTANCE.drop(paths)) ci.cancel();
    }
}
