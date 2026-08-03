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
    private static long lastPrintTime = 0;

    @Inject(method = "getBlockBreakingSpeed", at = @At("HEAD"), cancellable = true, remap = true)
    private void onGetBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if (AutomationManager.isBuffEnabled()) {
            try {
                // Use reflection via MappingHelper for absolute robustness and Zero-Link safety
                Object inventory = MappingHelper.invokeMethod(this, "method_31548"); // getInventory()
                if (inventory != null) {
                    float speed = ((Number) MappingHelper.invokeMethod(inventory, "method_7370", blockState)).floatValue(); // getBlockBreakingSpeed(BlockState)
                    float buffedSpeed = speed;
                    if (speed > 1.0f) {
                        // 恒定效率 X (Efficiency 10): level * level + 1 = 101.0f
                        buffedSpeed += 101.0f;
                    }
                    // 信标效果急迫 X (Haste 10): multiplier = 1.0f + 0.2f * 10 = 3.0f
                    buffedSpeed *= 3.0f;

                    long now = System.currentTimeMillis();
                    if (now - lastPrintTime > 1000) {
                        lastPrintTime = now;
                        net.mcc.CommandDispatcher.addFeedback("§d[MCC Debug] speed: " + speed + " -> buffed: " + buffedSpeed);
                    }

                    cir.setReturnValue(buffedSpeed);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
