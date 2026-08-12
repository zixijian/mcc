package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("HEAD"), cancellable = true)
    private void onGetBlockBreakingSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        if (AutomationManager.isMiningBuffActive()) {
            try {
                PlayerEntity player = (PlayerEntity) (Object) this;
                PlayerInventory inventory = player.getInventory();
                if (inventory != null) {
                    float baseSpeed = inventory.getBlockBreakingSpeed(blockState);
                    float speed = baseSpeed;
                    if (speed > 1.0f) {
                        speed += 26.0f; // Efficiency 5 (5*5 + 1)
                    }
                    speed *= 1.4f; // Haste 2 (1 + 2 * 0.2)
                    System.out.println("[MCC] getBlockBreakingSpeed called! baseSpeed: " + baseSpeed + ", speed: " + speed);
                    cir.setReturnValue(speed);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
