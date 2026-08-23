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

    @Inject(
        method = {
            "getBlockBreakingSpeed",
            "getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F",
            "getBlockBreakingSpeed(Lnet/minecraft/class_2680;)F",
            "method_7351(Lnet/minecraft/class_2680;)F",
            "method_7351"
        },
        at = @At("RETURN"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private void onGetBlockBreakingSpeed(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        if (!AutomationManager.isBuffEnabled()) {
            return;
        }

        try {
            float speed = cir.getReturnValueF();
            Object player = this;

            // 1. 消除飞行/空中/游泳挖掘减速惩罚 (默认设为 true，防止反射失败误判为空中)
            boolean onGround = true;
            try {
                Object val = MappingHelper.invokeMethod(player, "isOnGround");
                if (val instanceof Boolean) {
                    onGround = (Boolean) val;
                }
            } catch (Exception ignored) {}

            if (!onGround) {
                speed *= 5.0f;
            }

            // 2. 按照“基础速度 × 效率V (26倍) × 急迫II (1.4倍)”曲线计算增幅
            if (speed > 1.0f) {
                if (speed < 27.0f) {
                    speed += 26.0f;
                }
                speed *= 1.4f;
            } else if (speed > 0.0f) {
                speed *= 1.4f;
            }

            // 3. 合法上限约束：单次包内/Tick破坏进度增量不得超过理论最大瞬时增量 (急迫II + 效率V + 满级工具 53.2f)
            float maxTheoreticalSpeed = 53.2f;
            if (speed > maxTheoreticalSpeed) {
                speed = maxTheoreticalSpeed;
            }

            cir.setReturnValue(speed);
        } catch (Throwable ignored) {}
    }
}
