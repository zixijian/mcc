package net.mcc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_1309") // LivingEntity
public abstract class LivingEntityMixin {

    // 针对 1.21.1/1.21.4 的 damage(DamageSource, float) 方法签名适配
    // method_6045 对应 LivingEntity.damage
    @Inject(method = "method_6045(Lnet/minecraft/class_1282;F)Z", at = @At("HEAD"), remap = false)
    private void onDamage(Object source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            // DamageSource.getAttacker(): method_5529
            java.lang.reflect.Method getAttacker = source.getClass().getMethod("method_5529");
            Object attacker = getAttacker.invoke(source);

            if (attacker != null && attacker.getClass().getName().contains("class_1657")) {
                Class<?> curr = this.getClass();
                while (curr != null && !curr.getName().equals("java.lang.Object")) {
                    for (java.lang.reflect.Field f : curr.getDeclaredFields()) {
                        if (f.getName().equals("field_6008") || f.getName().equals("hurtResistantTime")) {
                            f.setAccessible(true);
                            f.setInt(this, 0);
                            return;
                        }
                    }
                    curr = curr.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
    }
}
