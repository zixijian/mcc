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

        // 核心提速逻辑：
        // 在当前状态（包括地面与飞行/空中）的基础挖掘速度之上，进行 1.41f 极限加速。
        // 该系数精准匹配服务端 STOP_DESTROY_BLOCK 数据包校验的 0.7f 判定下限（1 / 0.7 = 1.428），
        // 既实现了最高效率的挖掘速度，又绝对保证服务端 100% 接受发包，彻底解决飞行挖掘闪动与幽灵方块问题。
        cir.setReturnValue(baseSpeed * 1.41f);
    }
}
