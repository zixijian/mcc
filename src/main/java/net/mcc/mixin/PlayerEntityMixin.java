package net.mcc.mixin;

import net.mcc.AutomationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_1657") // PlayerEntity
public class PlayerEntityMixin {

    // Yarn named method
    @Inject(method = "getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeedNamed(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        applyBuff(cir);
    }

    // Intermediary method
    @Inject(method = "method_7351(Lnet/minecraft/class_2680;)F", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeedIntermediary(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        applyBuff(cir);
    }

    private void applyBuff(CallbackInfoReturnable<Float> cir) {
        if (!AutomationManager.isBuffEnabled()) return;

        float baseSpeed = cir.getReturnValue();
        if (baseSpeed <= 0.0f) return;

        // 为保证符合服务端 STOP_DESTROY_BLOCK 数据包校验 (0.7f 判定阈值) 且绝不产生幽灵方块/闪动，
        // 将当前状态下的基础挖掘速度提速 1.41f (41% 最高合法提速)。
        // 在站立或飞行状态下均能无缝通过服务端校验，完美实现有效挖掘与加速。
        cir.setReturnValue(baseSpeed * 1.41f);
    }
}
