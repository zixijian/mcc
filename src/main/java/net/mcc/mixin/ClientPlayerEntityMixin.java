package net.mcc.mixin;

import net.mcc.CommandDispatcher;
import net.mcc.AutomationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.player.LocalPlayer")
public class ClientPlayerEntityMixin {

    @Inject(method = {"tick", "method_3110", "method_5773"}, at = @At("HEAD"), remap = false, require = 0)
    private void onTickPre(CallbackInfo ci) {
        AutomationManager.onClientTick();
    }

    @Inject(method = {"sendCommand(Ljava/lang/String;)V", "method_63668"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendCommandVoid(String command, CallbackInfo ci) {
        if (CommandDispatcher.dispatch("/" + command)) {
            ci.cancel();
        }
    }

    @Inject(method = {"sendCommand(Ljava/lang/String;)Z", "method_3111"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendCommandBoolean(String command, CallbackInfoReturnable<Boolean> cir) {
        if (CommandDispatcher.dispatch("/" + command)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = {"sendChat", "chat", "sendChatMessage", "method_3143", "method_63667"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message != null && message.startsWith("/mcc")) {
            if (CommandDispatcher.dispatch(message)) {
                ci.cancel();
            }
        }
    }
}
