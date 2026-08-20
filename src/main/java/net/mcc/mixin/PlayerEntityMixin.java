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

        float speed = cir.getReturnValue();
        if (speed <= 0.0f) return;

        // 1. 移除飞行/空中挖掘减速效果：如果玩家不在地面 (!isOnGround)，乘以 5.0f 抵消除以 5 的原生减速
        boolean onGround = true;
        try {
            onGround = (boolean) MappingHelper.invokeMethod(this, "isOnGround");
        } catch (Exception e1) {
            try {
                onGround = (boolean) MappingHelper.getFieldValue(this, "field_6017", null);
            } catch (Exception ignored) {}
        }

        if (!onGround) {
            speed *= 5.0f;
        }

        // 2. 效率 5 (Efficiency 5): 当工具匹配 (speed > 1.0f) 时，增加 26.0f 速度
        if (speed > 1.0f) {
            speed += 26.0f;
        }

        // 3. 信标急迫 2 (Haste 2): 速度乘以 1.4f (40% 提升)
        speed *= 1.4f;

        cir.setReturnValue(speed);
    }
}
