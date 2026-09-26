package net.mcc.mixin;

import net.mcc.MappingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent")
public class ChatHudMixin {

    @Inject(method = {"addRecentChat(Ljava/lang/String;)V", "method_1812(Ljava/lang/String;)V"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onAddToHistoryString(String message, CallbackInfo ci) {
        if (message != null && message.startsWith("/mcc")) {
            ci.cancel();
        }
    }

    @Inject(method = {"addRecentChat", "method_1812"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onAddToHistoryText(@Coerce Object message, CallbackInfo ci) {
        try {
            String content = (String) MappingHelper.invokeMethod(message, "getString");
            if (content != null && content.startsWith("/mcc")) {
                ci.cancel();
            }
        } catch (Exception e) {}
    }
}
