package net.mcc.mixin;

import net.mcc.AutomationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        if (!AutomationManager.isBuffEnabled() || state == null) {
            return;
        }

        try {
            Object player = this;

            // 1. Locate PlayerInventory object on PlayerEntity
            Object inventory = null;
            try {
                Class<?> curr = player.getClass();
                while (curr != null && curr != Object.class) {
                    for (Field f : curr.getDeclaredFields()) {
                        if (f.getType().getName().contains("PlayerInventory") || f.getType().getName().contains("class_1661")) {
                            f.setAccessible(true);
                            inventory = f.get(player);
                            if (inventory != null) break;
                        }
                    }
                    if (inventory != null) break;
                    curr = curr.getSuperclass();
                }
            } catch (Throwable ignored) {}

            // 2. Fetch tool base breaking speed for target BlockState safely
            float baseSpeed = 1.0f;
            if (inventory != null) {
                for (Method m : inventory.getClass().getDeclaredMethods()) {
                    if (m.getReturnType() == float.class && m.getParameterCount() == 1) {
                        if (m.getParameterTypes()[0].isInstance(state)) {
                            try {
                                m.setAccessible(true);
                                Object res = m.invoke(inventory, state);
                                if (res instanceof Number) {
                                    baseSpeed = ((Number) res).floatValue();
                                    break;
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            float speed = baseSpeed;

            // 3. Check if player holds an item in main hand
            boolean holdingItem = false;
            try {
                Object mainHandStack = null;
                Class<?> pCurr = player.getClass();
                while (pCurr != null && pCurr != Object.class) {
                    for (Method m : pCurr.getDeclaredMethods()) {
                        if ((m.getName().equals("getMainHandStack") || m.getName().equals("method_6047")) && m.getParameterCount() == 0) {
                            m.setAccessible(true);
                            mainHandStack = m.invoke(player);
                            break;
                        }
                    }
                    if (mainHandStack != null) break;
                    pCurr = pCurr.getSuperclass();
                }

                if (mainHandStack != null) {
                    for (Method m : mainHandStack.getClass().getDeclaredMethods()) {
                        if ((m.getName().equals("isEmpty") || m.getName().equals("method_7960")) && m.getParameterCount() == 0) {
                            try {
                                m.setAccessible(true);
                                holdingItem = !((Boolean) m.invoke(mainHandStack));
                                break;
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Efficiency 5 bonus (+26.0f)
            if (speed > 1.0f || holdingItem) {
                speed += 26.0f;
            }

            // Haste 2 multiplier (* 1.4f)
            speed *= 1.4f;

            // Bypass underwater (/5.0f) and airborne/flight (/5.0f) speed reductions
            cir.setReturnValue(speed);
        } catch (Throwable t) {
            // Fallback: do not cancel if any exception occurs
        }
    }
}
