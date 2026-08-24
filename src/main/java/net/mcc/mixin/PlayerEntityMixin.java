package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.CommandDispatcher;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    private void onGetBlockBreakingSpeed(@Coerce Object blockStateObj, CallbackInfoReturnable<Float> cir) {
        if (!AutomationManager.isBuffEnabled()) {
            return;
        }

        try {
            float speed = cir.getReturnValueF();
            if (speed <= 0.0f) return;

            PlayerEntity player = (PlayerEntity) (Object) this;

            // 1. 消除飞行/空中/游泳挖掘减速惩罚：
            // 如果玩家不在地面 (!isOnGround)，原版 getBlockBreakingSpeed 将速度除以了 5.0f。
            // 因此此处将其乘回 5.0f，将飞行/空中速度还原至地面挖掘速度。
            if (!player.isOnGround()) {
                speed *= 5.0f;
            }

            // 2. 按照“效率V (26倍) + 急迫II (1.4倍)”等效逻辑提升速度：
            if (speed > 1.0f) {
                if (speed < 27.0f) {
                    speed += 26.0f;
                }
                speed *= 1.4f;
            } else if (speed > 0.0f) {
                speed *= 1.4f;
            }

            // 3. 理论最大合法上限约束 (53.2f)
            if (speed > 53.2f) {
                speed = 53.2f;
            }

            // 4. 结合服务端 70% 破坏校验容忍门槛计算安全 Tick 周期，防止服务端因进度不足拒绝 STOP 数据包引发方块闪烁/回弹
            Object clientObj = CommandDispatcher.getClient();
            if (clientObj != null) {
                net.minecraft.client.MinecraftClient client = (net.minecraft.client.MinecraftClient) clientObj;
                HitResult hit = client.crosshairTarget;
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos();
                    World world = client.world;
                    if (pos != null && world != null) {
                        BlockState state = world.getBlockState(pos);
                        if (state != null) {
                            boolean isSuitable = player.canHarvest(state);
                            float divisor = isSuitable ? 30.0f : 100.0f;
                            float hardness = state.getHardness(world, pos);

                            if (hardness > 0.0f) {
                                // 计算真实服务端单 Tick 进度增量 (基于原版未 Buff 的基础速度)
                                float rawBaseSpeed = cir.getReturnValueF();
                                float dServer = rawBaseSpeed / hardness / divisor;

                                if (dServer > 0.0f) {
                                    // 服务端达到 0.70f 校验门槛所需的最小安全 Tick 数
                                    int nSafe = (int) Math.ceil(0.70f / dServer);
                                    if (nSafe < 1) nSafe = 1;

                                    // 计算 Buff 加成后的单 Tick 增量
                                    float dBuffed = speed / hardness / divisor;
                                    int nBuffed = (int) Math.ceil(1.0f / dBuffed);
                                    if (nBuffed < 1) nBuffed = 1;

                                    // 最终安全周期：不能早于服务端的 nSafe 周期
                                    int nFinal = Math.max(nBuffed, nSafe);

                                    float finalDelta = 1.0f / nFinal;
                                    float finalSpeed = finalDelta * hardness * divisor;

                                    if (finalSpeed > speed) finalSpeed = speed;
                                    if (finalSpeed > 0.0f) {
                                        cir.setReturnValue(finalSpeed);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            cir.setReturnValue(speed);
        } catch (Throwable ignored) {}
    }
}
