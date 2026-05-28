package net.mcc.mixin;

import net.mcc.CommandDispatcher;
import net.mcc.AutomationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 拦截客户端发送的命令并处理 Tick
 */
@Mixin(targets = "net.minecraft.class_746") // ClientPlayerEntity
public class ClientPlayerEntityMixin {

    // tick (1.21.1)
    @Inject(method = "method_3110", at = @At("HEAD"), remap = false, require = 0)
    private void onTickPre(CallbackInfo ci) {
        AutomationManager.onClientTick();
    }

    // tick (1.21.2+)
    @Inject(method = "method_5773", at = @At("HEAD"), remap = false, require = 0)
    private void onTickPreNew(CallbackInfo ci) {
        AutomationManager.onClientTick();
    }

    // tickMovement (1.21.x) - method_3140 在某些版本返回 void
    @Inject(method = "method_3140()V", at = @At("TAIL"), remap = false, require = 0)
    private void onTickMovementPostVoid(CallbackInfo ci) {
        AutomationManager.onPlayerTick();
    }

    // tickMovement (1.21.x) - method_3140 在某些版本返回 boolean
    @Inject(method = "method_3140()Z", at = @At("TAIL"), remap = false, require = 0)
    private void onTickMovementPostReturnable(CallbackInfoReturnable<Boolean> cir) {
        AutomationManager.onPlayerTick();
    }

    // 针对 1.21.2+ 的 Input.tick() 注入
    // 1.21.1: method_3129()V
    @Inject(method = "method_3140", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_744;method_3129()V", shift = At.Shift.AFTER), remap = false, require = 0)
    private void onInputTickPost(CallbackInfo ci) {
        AutomationManager.onPlayerTick();
    }

    // 1.21.2+: method_3129(ZF)V
    @Inject(method = "method_3140", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_744;method_3129(ZF)V", shift = At.Shift.AFTER), remap = false, require = 0)
    private void onInputTickPostNew(CallbackInfo ci) {
        AutomationManager.onPlayerTick();
    }

    // 1.21.1 sendCommand (返回 boolean)
    @Inject(method = "method_3111(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendCommand1211(String command, CallbackInfoReturnable<Boolean> cir) {
        if (CommandDispatcher.dispatch("/" + command)) {
            cir.setReturnValue(true);
        }
    }

    // 1.21.2+ sendCommand (返回 void)
    @Inject(method = "method_63668(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendCommand1212(String command, CallbackInfo ci) {
        if (CommandDispatcher.dispatch("/" + command)) {
            ci.cancel();
        }
    }

    // 1.21.1 sendChatMessage (返回 void)
    @Inject(method = "method_3143(Ljava/lang/String;Lnet/minecraft/class_2561;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendChatMessage1211(String message, @Coerce Object component, CallbackInfo ci) {
        if (message.startsWith("/mcc")) {
            if (CommandDispatcher.dispatch(message)) {
                ci.cancel();
            }
        }
    }

    // 1.21.2+ sendChatMessage (返回 void)
    @Inject(method = "method_63667(Ljava/lang/String;Lnet/minecraft/class_2561;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendChatMessage1212(String message, @Coerce Object component, CallbackInfo ci) {
        if (message.startsWith("/mcc")) {
            if (CommandDispatcher.dispatch(message)) {
                ci.cancel();
            }
        }
    }
}
