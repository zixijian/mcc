package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.MappingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.class_1657", "net.minecraft.class_746"}) // PlayerEntity & ClientPlayerEntity
public class PlayerEntityMixin {

    // Yarn named method
    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeedNamed(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        applyBuff(blockState, cir);
    }

    // Intermediary method
    @Inject(method = "method_7351", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeedIntermediary(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        applyBuff(blockState, cir);
    }

    private void applyBuff(Object blockState, CallbackInfoReturnable<Float> cir) {
        if (!AutomationManager.isBuffEnabled()) return;

        float baseSpeed = cir.getReturnValue();
        if (baseSpeed <= 0.0f) return;

        float rawSpeed = -1.0f;

        // 1. 直接获取当前手持工具的基础挖掘速度 (例如钻石镐为 6.0f)
        // 彻底消除空中 (airborne/flying) 和水下 (underwater) 的 5 倍原生减速除法
        try {
            Object player = this;
            Object inv = MappingHelper.getFieldValue(player, "inventory", null);
            if (inv != null) {
                int selectedSlot = ((Number) MappingHelper.getFieldValue(inv, "selectedSlot", null)).intValue();
                Object main = MappingHelper.getFieldValue(inv, "main", null);
                if (main instanceof java.util.List) {
                    Object stack = ((java.util.List<?>) main).get(selectedSlot);
                    if (stack != null && !(boolean) MappingHelper.invokeMethod(stack, "isEmpty")) {
                        Object res = MappingHelper.invokeMethod(stack, "getMiningSpeedMultiplier", blockState);
                        if (res instanceof Number) {
                            rawSpeed = ((Number) res).floatValue();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 2. 兜底逻辑：若未能从 stack 拿到原始速度，则还原 baseSpeed 中可能的空中 5 倍减速
        if (rawSpeed < 0.0f) {
            rawSpeed = baseSpeed;
            boolean onGround = true;
            try {
                onGround = (boolean) MappingHelper.invokeMethod(this, "isOnGround");
            } catch (Exception e1) {
                try {
                    onGround = (boolean) MappingHelper.getFieldValue(this, "field_6017", null);
                } catch (Exception ignored) {}
            }
            if (!onGround) {
                rawSpeed *= 5.0f;
            }
        }

        // 3. 效率 5 (Efficiency 5): 工具匹配 (rawSpeed > 1.0f) 时，加上 +26.0f 附加值
        if (rawSpeed > 1.0f) {
            if (rawSpeed < 27.0f) {
                rawSpeed += 26.0f;
            }
        }

        // 4. 信标急迫 2 (Haste 2): 速度乘以 1.4f (40% 提升)
        rawSpeed *= 1.4f;

        cir.setReturnValue(rawSpeed);
    }
}
