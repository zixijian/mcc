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

        // 挖掘 Buff 增强逻辑：
        // 在当前状态（站立、飞行、水下）的基础挖掘速度之上乘以 1.41f (41% 极限提速)。
        // 提速比例精确契合服务端 STOP_DESTROY_BLOCK 数据包校验的 0.70f 判定门槛 (1 / 1.41 = 0.709)，
        // 既提供了最快的挖掘加速，又 100% 保证服务端无缝接受破坏数据包，
        // 彻底解决黑石等高硬度方块在站立与飞行挖掘时的闪动与幽灵方块问题。
        cir.setReturnValue(baseSpeed * 1.41f);
    }
}
