package bobba.mod.client.mixin;

import bobba.mod.client.watchlist.ChatRightClickHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void bobbamod$onMouseClicked(MouseButtonEvent event, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 1) return;
        if (simulate) return;
        if (ChatRightClickHandler.handle(event.x(), event.y())) {
            cir.setReturnValue(true);
        }
    }
}
