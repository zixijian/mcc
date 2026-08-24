package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.CommandDispatcher;
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
            float actualSpeed = cir.getReturnValueF();

            // 1. 如果玩家不在地面 (!isOnGround)，原版 getBlockBreakingSpeed 将速度除以 5.0。
            // 因为我们在发包时向服务端伪装了 onGround = true，所以将 actualSpeed 乘回 5.0，还原地面实际挖掘速度。
            boolean onGround = true;
            try {
                Object val = MappingHelper.invokeMethod(this, "isOnGround");
                if (val instanceof Boolean) {
                    onGround = (Boolean) val;
                }
            } catch (Exception ignored) {}

            if (!onGround) {
                actualSpeed *= 5.0f;
            }

            if (actualSpeed <= 0.0f) {
                return;
            }

            // 2. 计算理想 Buff 速度 (效率V + 急迫II)
            float buffedSpeed = actualSpeed;
            if (buffedSpeed > 1.0f) {
                if (buffedSpeed < 27.0f) {
                    buffedSpeed += 26.0f;
                }
                buffedSpeed *= 1.4f;
            } else {
                buffedSpeed *= 1.4f;
            }

            // 限制在理论最大上限 53.2f 内
            if (buffedSpeed > 53.2f) {
                buffedSpeed = 53.2f;
            }

            // 3. 结合服务端 70% 破坏校验容忍度计算安全 Tick 周期，防止服务端因进度不足拒绝 STOP 数据包引发方块闪烁/回弹
            Object client = CommandDispatcher.getClient();
            if (client != null) {
                Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
                if (target != null && MappingHelper.getClass("BlockHitResult").isInstance(target)) {
                    Object pos = MappingHelper.invokeMethod(target, "getBlockPos");
                    Object world = CommandDispatcher.getClientWorld();
                    if (pos != null && world != null) {
                        Object targetState = MappingHelper.invokeMethod(world, "getBlockState", pos);
                        if (targetState != null) {
                            boolean isSuitable = false;
                            try {
                                isSuitable = (boolean) MappingHelper.invokeMethod(this, "canHarvest", targetState);
                            } catch (Exception e) { isSuitable = true; }

                            float divisor = isSuitable ? 30.0f : 100.0f;
                            float hardness = 1.0f;
                            try {
                                Object block = MappingHelper.invokeMethod(targetState, "getBlock");
                                hardness = ((Number) MappingHelper.getFieldValue(block, "hardness", null)).floatValue();
                            } catch (Exception e) {
                                try {
                                    hardness = ((Number) MappingHelper.invokeMethod(targetState, "getHardness", world, pos)).floatValue();
                                } catch (Exception ignored) {}
                            }

                            if (hardness > 0.0f) {
                                float dActual = actualSpeed / hardness / divisor;
                                float dBuffed = buffedSpeed / hardness / divisor;

                                if (dBuffed >= 1.0f && dActual >= 0.70f) {
                                    cir.setReturnValue(buffedSpeed);
                                    return;
                                }

                                int nActual = (int) Math.ceil(0.70f / dActual);
                                int nBuffed = (int) Math.ceil(1.0f / dBuffed);
                                int nSafe = Math.max(1, Math.max(nBuffed, nActual));

                                float safeDelta = 1.0f / nSafe;
                                float safeSpeed = safeDelta * hardness * divisor;

                                if (safeSpeed < actualSpeed) safeSpeed = actualSpeed;
                                if (safeSpeed > buffedSpeed) safeSpeed = buffedSpeed;

                                cir.setReturnValue(safeSpeed);
                                return;
                            }
                        }
                    }
                }
            }

            cir.setReturnValue(buffedSpeed);
        } catch (Throwable ignored) {}
    }
}
