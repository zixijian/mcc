package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.MappingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("HEAD"), cancellable = true, remap = true)
    private void onGetBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if (AutomationManager.isBuffEnabled()) {
            try {
                // Use reflection via MappingHelper for absolute robustness and Zero-Link safety
                Object inventory = MappingHelper.invokeMethod(this, "method_31548"); // getInventory()
                if (inventory != null) {
                    float speed = ((Number) MappingHelper.invokeMethod(inventory, "method_7370", blockState)).floatValue(); // getBlockBreakingSpeed(BlockState)
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
