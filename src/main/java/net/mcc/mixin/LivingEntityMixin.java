package net.mcc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_1309") // LivingEntity
public abstract class LivingEntityMixin {

    /**
     * 使用官方名称 "hurt" 注入。用户反馈在 1.21.x 下此名称一致。
     * 使用 remap = false 以避免 Loom 尝试将其映射回 intermediary 名（如果环境特殊）。
     * 同时保留反射兜底以增强鲁棒性。
     */
    @Inject(method = "hurt", at = @At("HEAD"), remap = false, cancellable = true)
    private void onHurt(Object source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            // 1. 获取攻击者 (getAttacker / method_5529)
            Object attacker = null;
            try {
                attacker = source.getClass().getMethod("getAttacker").invoke(source);
            } catch (Exception e) {
                try {
                    attacker = source.getClass().getMethod("method_5529").invoke(source);
                } catch (Exception ignored) {}
            }

            // 2. 检查是否为玩家 (PlayerEntity / class_1657)
            if (attacker != null) {
                String name = attacker.getClass().getName();
                if (name.contains("Player") || name.contains("class_1657")) {
                    // 3. 强制重置受击无敌时间 (hurtResistantTime / field_6008)
                    // 遍历父类查找字段，解决 Shadow 无法定位父类字段的问题
                    Class<?> curr = this.getClass();
                    while (curr != null && !curr.getName().equals("java.lang.Object")) {
                        for (java.lang.reflect.Field f : curr.getDeclaredFields()) {
                            if (f.getName().equals("hurtResistantTime") || f.getName().equals("field_6008")) {
                                f.setAccessible(true);
                                f.setInt(this, 0);
                                return;
                            }
                        }
                        curr = curr.getSuperclass();
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
