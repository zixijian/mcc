package net.mcc.mixin;

import net.mcc.AutomationManager;
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

        // 挖掘 Buff 加速效果：
        // 在当前站立或飞行基础挖掘速度之上乘以 1.30f (30% 稳定提速)。
        // 该比例保持服务端进度 >= 0.77f，完美处于 0.70f 判定安全阈值之上，
        // 既提供明显的挖掘加速效果，又 100% 避免黑石及飞行挖掘时的闪动与幽灵方块。
        cir.setReturnValue(baseSpeed * 1.30f);
    }
}
