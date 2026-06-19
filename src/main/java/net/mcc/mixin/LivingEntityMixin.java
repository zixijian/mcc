package net.mcc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_1309") // LivingEntity
public abstract class LivingEntityMixin {

    @Inject(method = "method_6045", at = @At("HEAD"), remap = false) // damage(DamageSource, float)
    private void onDamage(Object source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            // 通过反射获取攻击者，避免类加载冲突
            // DamageSource.getAttacker(): method_5529
            java.lang.reflect.Method getAttacker = source.getClass().getMethod("method_5529");
            Object attacker = getAttacker.invoke(source);

            // 如果攻击者是玩家 (class_1657)
            if (attacker != null && attacker.getClass().getName().contains("class_1657")) {
                // 暴力通过反射修改 hurtResistantTime (field_6008)，它在 Entity (class_1297) 中定义
                // 遍历父类查找字段以增强兼容性
                Class<?> curr = this.getClass();
                while (curr != null && curr != Object.class) {
                    try {
                        java.lang.reflect.Field f = curr.getDeclaredField("field_6008");
                        f.setAccessible(true);
                        f.setInt(this, 0);
                        break;
                    } catch (NoSuchFieldException ignored) {
                        curr = curr.getSuperclass();
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
