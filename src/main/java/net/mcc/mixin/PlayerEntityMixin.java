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
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private void onGetBlockBreakingSpeed(@Coerce Object state, CallbackInfoReturnable<Float> cir) {
        if (!AutomationManager.isBuffEnabled()) {
            return;
        }

        try {
            Object player = this;
            // Get base breaking speed from player's inventory using state
            // PlayerInventory.getBlockBreakingSpeed(BlockState) -> method_7370
            float baseSpeed = 1.0f;
            try {
                Object inventory = MappingHelper.getFieldValue(player, "inventory", null);
                if (inventory != null) {
                    baseSpeed = ((Number) MappingHelper.invokeMethod(inventory, "method_7370", state)).floatValue();
                }
            } catch (Exception e1) {
                try {
                    Object inventory = MappingHelper.getFieldValue(player, "inventory", null);
                    if (inventory != null) {
                        baseSpeed = ((Number) MappingHelper.invokeMethod(inventory, "getBlockBreakingSpeed", state)).floatValue();
                    }
                } catch (Exception e2) {
                    baseSpeed = 1.0f;
                }
            }

            float speed = baseSpeed;

            // Efficiency 5 logic: if tool is suitable for block (baseSpeed > 1.0f), add efficiency bonus (lvl*lvl + 1 = 26)
            if (speed > 1.0f) {
                speed += 26.0f; // Efficiency 5
            }

            // Haste 2 logic: multiply speed by (1.0f + 0.2f * amplifier) = 1.4f for Haste 2
            speed *= 1.4f;

            // Note: We deliberately do NOT divide speed by 5.0f for underwater or airborne states,
            // effectively removing underwater and flight/airborne mining slowdown.

            cir.setReturnValue(speed);
        } catch (Throwable t) {
            // Fallback: do not cancel if any reflection exception occurs
        }
    }
}
