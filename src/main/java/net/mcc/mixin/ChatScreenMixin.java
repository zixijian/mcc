package net.mcc.mixin;

import net.mcc.CommandDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_408") // ChatScreen
public class ChatScreenMixin {
    // 1.21.1+ sendMessage 返回 boolean
    @Inject(method = "method_2108(Ljava/lang/String;Z)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (message != null && message.startsWith("/mcc")) {
            if (CommandDispatcher.dispatch(message)) {
                cir.setReturnValue(true);
            }
        }
    }

    // 适配不带 boolean 参数的版本 (如果有的话)
    @Inject(method = "method_2108(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendMessageOld(String message, CallbackInfoReturnable<Boolean> cir) {
        if (message != null && message.startsWith("/mcc")) {
            if (CommandDispatcher.dispatch(message)) {
                cir.setReturnValue(true);
            }
        }
    }
}
