package net.mcc.mixin;

import net.mcc.AutomationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.class_310") // MinecraftClient
public class MinecraftClientMixin {
    @Inject(method = "method_1574", at = @At("HEAD"), remap = false, require = 0)
    private void onTick(CallbackInfo ci) {
        try {
            AutomationManager.onClientTick();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
