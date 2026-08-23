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
        applyBuff(cir);
    }

    // Intermediary method
    @Inject(method = "method_7351", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeedIntermediary(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        applyBuff(cir);
    }

    private void applyBuff(CallbackInfoReturnable<Float> cir) {
        if (!AutomationManager.isBuffEnabled()) return;

        float baseSpeed = cir.getReturnValue();
        if (baseSpeed <= 0.0f) return;

        float rawSpeed = baseSpeed;

        // 1. 移除飞行/空中挖掘减速惩罚：
        // 原生 getBlockBreakingSpeed 在玩家不在地面 (!isOnGround) 时会在末尾除以 5.0f。
        // 此处检测到不在地面时乘以 5.0f，彻底抵消除以 5 的飞行减速，使飞行挖掘速度与地面完全一致。
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

        // 2. 效率 5 (Efficiency 5): 当使用匹配工具 (rawSpeed > 1.0f) 且未包含效率 5 附加值 (rawSpeed < 27.0f) 时，增加 26.0f
        if (rawSpeed > 1.0f && rawSpeed < 27.0f) {
            rawSpeed += 26.0f;
        }

        // 3. 信标急迫 2 (Haste 2): 速度乘以 1.4f (40% 提升)
        rawSpeed *= 1.4f;

        cir.setReturnValue(rawSpeed);
    }
}
