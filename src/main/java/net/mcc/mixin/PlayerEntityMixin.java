package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.MappingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

/**
 * Mixin into PlayerEntity to override block breaking speed when /mcc buff is active.
 * Applies Efficiency V (+26.0f) and Haste II (*1.4f) while removing airborne and underwater slowdowns.
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
            float speed = cir.getReturnValue();
            Object player = this;

            // 1. Check if player is on ground
            boolean onGround = true;
            try {
                Object res = MappingHelper.invokeMethod(player, "isOnGround");
                if (res instanceof Boolean) onGround = (Boolean) res;
            } catch (Throwable e) {
                try {
                    Field f = MappingHelper.findField(player.getClass(), "field_6012");
                    onGround = f.getBoolean(player);
                } catch (Throwable ignored) {}
            }

            // Remove vanilla 5x airborne mining speed penalty when in mid-air
            if (!onGround) {
                speed *= 5.0f;
            }

            // 2. Check if player is submerged in water
            boolean submerged = false;
            try {
                Object res = MappingHelper.invokeMethod(player, "isSubmergedInWater");
                if (res instanceof Boolean) submerged = (Boolean) res;
            } catch (Throwable e) {
                try {
                    Field f = MappingHelper.findField(player.getClass(), "field_6000");
                    submerged = f.getBoolean(player);
                } catch (Throwable ignored1) {
                    try {
                        Field f2 = MappingHelper.findField(player.getClass(), "field_5973");
                        submerged = f2.getBoolean(player);
                    } catch (Throwable ignored2) {}
                }
            }

            // Remove vanilla 5x underwater mining speed penalty when underwater
            if (submerged) {
                speed *= 5.0f;
            }

            // 3. Efficiency 5 bonus (+26.0f) applied if tool is suitable for block (speed > 1.0f)
            if (speed > 1.0f) {
                speed += 26.0f;
            }

            // 4. Haste 2 multiplier (* 1.4f)
            speed *= 1.4f;

            cir.setReturnValue(speed);
        } catch (Throwable t) {
            // Fallback
        }
    }
}
