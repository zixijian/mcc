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
        "getBlockBreakingSpeed",
        "method_7351"
    }, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeed(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        if (AutomationManager.isMiningBuffActive()) {
            try {
                Object inventory = MappingHelper.getFieldValue(this, "inventory", null);
                if (inventory != null) {
                    float baseSpeed = 1.0f;

                    // Search for getBlockBreakingSpeed or method_7370 by name only to be 100% robust
                    java.lang.reflect.Method targetMethod = null;
                    for (java.lang.reflect.Method m : inventory.getClass().getMethods()) {
                        if (m.getName().equals("getBlockBreakingSpeed") || m.getName().equals("method_7370")) {
                            targetMethod = m;
                            break;
                        }
                    }
                    if (targetMethod == null) {
                        for (java.lang.reflect.Method m : inventory.getClass().getDeclaredMethods()) {
                            if (m.getName().equals("getBlockBreakingSpeed") || m.getName().equals("method_7370")) {
                                targetMethod = m;
                                break;
                            }
                        }
                    }

                    if (targetMethod != null) {
                        targetMethod.setAccessible(true);
                        baseSpeed = ((Number) targetMethod.invoke(inventory, blockState)).floatValue();
                    }

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
