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

    @Inject(method = "method_7351(Lnet/minecraft/class_2680;)F", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeed(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        if (AutomationManager.isBuffEnabled()) {
            try {
                // Get player inventory: PlayerEntity.getInventory() -> method_31548()
                Object inventory = MappingHelper.invokeMethod(this, "method_31548");
                if (inventory != null) {
                    // Get base breaking speed from PlayerInventory: method_7370(BlockState)
                    float speed = ((Number) MappingHelper.invokeMethod(inventory, "method_7370", blockState)).floatValue();
                    if (speed > 1.0f) {
                        // 恒定效率 X (Efficiency 10): level * level + 1 = 101.0f
                        speed += 101.0f;
                    }
                    // 信标效果急迫 X (Haste 10): multiplier = 1.0f + 0.2f * 10 = 3.0f
                    speed *= 3.0f;

                    cir.setReturnValue(speed);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
