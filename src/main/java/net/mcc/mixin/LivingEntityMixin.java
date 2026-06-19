package net.mcc.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_1309") // LivingEntity
public abstract class LivingEntityMixin {
    @Shadow(remap = false) public int field_6008; // hurtResistantTime

    @Inject(method = "method_6045", at = @At("HEAD"), remap = false) // damage(DamageSource, float)
    private void onDamage(Object source, float amount, CallbackInfoReturnable<Boolean> cir) {
        try {
            // 通过反射判断 source.getAttacker() 是否为玩家
            java.lang.reflect.Method getAttacker = source.getClass().getMethod("method_5529");
            Object attacker = getAttacker.invoke(source);
            if (attacker != null && attacker.getClass().getName().contains("class_1657")) { // PlayerEntity
                this.field_6008 = 0;
            }
        } catch (Exception ignored) {}
    }
}
