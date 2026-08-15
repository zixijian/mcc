package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.MappingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into PlayerEntity to override block breaking speed when /mcc buff is active.
 * Calculates Efficiency V (+26.0f when suitable) and Haste II (*1.4f multiplier)
 * while bypassing airborne and underwater mining speed reductions.
 */
@Mixin(targets = "net.minecraft.class_1657") // PlayerEntity
public class PlayerEntityMixin {

    // getBlockBreakingSpeed(BlockState) -> float
    // Yarn: getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F
    // Intermediary: method_7351(Lnet/minecraft/class_2680;)F
    @Inject(
        method = {
            "getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F",
            "method_7351(Lnet/minecraft/class_2680;)F"
        },
        at = @At("RETURN"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private void onGetBlockBreakingSpeedPost(@Coerce Object state, CallbackInfoReturnable<Float> cir) {
        if (!AutomationManager.isBuffEnabled() || state == null) {
            return;
        }

        try {
            Object player = this;

            // 1. Get base tool breaking speed from inventory (method_7370 / getBlockBreakingSpeed)
            float baseSpeed = 1.0f;
            try {
                Object inventory = MappingHelper.getFieldValue(player, "inventory", null);
                if (inventory != null) {
                    Object res = MappingHelper.invokeMethod(inventory, "getBlockBreakingSpeed", state);
                    if (res instanceof Number) {
                        baseSpeed = ((Number) res).floatValue();
                    }
                }
            } catch (Throwable ignored) {}

            float speed = baseSpeed;

            // 2. Efficiency 5 (+26.0f) applied ONLY when tool is suitable for block (baseSpeed > 1.0f)
            if (speed > 1.0f) {
                speed += 26.0f; // Efficiency 5 bonus
            }

            // 3. Haste 2 multiplier (* 1.4f)
            speed *= 1.4f;

            // Bypasses underwater (/5.0f) and airborne/flight (/5.0f) penalties natively
            cir.setReturnValue(speed);
        } catch (Throwable t) {
            // Fallback
        }
    }
}
