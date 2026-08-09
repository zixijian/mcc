package net.mcc.mixin;

import net.mcc.AutomationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.class_1657") // PlayerEntity
public class PlayerEntityMixin {

    @Inject(method = {
        "getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F",
        "method_7351(Lnet/minecraft/class_2680;)F"
    }, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onGetBlockBreakingSpeed(@Coerce Object blockState, CallbackInfoReturnable<Float> cir) {
        if (AutomationManager.isMiningBuffActive()) {
            try {
                net.minecraft.entity.player.PlayerEntity player = (net.minecraft.entity.player.PlayerEntity)(Object)this;
                net.minecraft.entity.player.PlayerInventory inventory = player.getInventory();
                if (inventory != null) {
                    float baseSpeed = inventory.getBlockBreakingSpeed((net.minecraft.block.BlockState) blockState);
                    float speed = baseSpeed;
                    if (speed > 1.0f) {
                        speed += 26.0f; // Efficiency 5 (5*5 + 1)
                    }
                    speed *= 1.4f; // Haste 2 (1 + 2 * 0.2)
                    cir.setReturnValue(speed);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
