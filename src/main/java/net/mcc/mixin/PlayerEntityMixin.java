package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.MappingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_1657") // PlayerEntity
public class PlayerEntityMixin {

    @Inject(method = {
        "getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F",
        "method_7351(Lnet/minecraft/class_2680;)F"
    }, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeed(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        if (AutomationManager.isMiningBuffActive()) {
            try {
                Object inventory = MappingHelper.getFieldValue(this, "inventory", null);
                if (inventory != null) {
                    float baseSpeed = ((Number) MappingHelper.invokeMethod(inventory, "getBlockBreakingSpeed", blockState)).floatValue();
                    float speed = baseSpeed;
                    if (speed > 1.0f) {
                        speed += 26.0f; // Efficiency 5 (5*5 + 1)
                    }
                    speed *= 1.4f; // Haste 2 (1 + 2 * 0.2)
                    cir.setReturnValue(speed);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
