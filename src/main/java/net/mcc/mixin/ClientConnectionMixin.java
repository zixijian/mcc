package net.mcc.mixin;

import net.mcc.AutomationManager;
import net.mcc.CommandDispatcher;
import net.mcc.MappingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Mixin into ClientConnection to modify outgoing movement packets during block breaking when /mcc buff is active.
 * Guarantees legal packet-level onGround synchronization for server-side mining validation.
 */
@Mixin(targets = "net.minecraft.class_2535") // ClientConnection
public class ClientConnectionMixin {

    // send(Packet)
    // Yarn: send(Lnet/minecraft/network/packet/Packet;)V
    // Intermediary: method_10743(Lnet/minecraft/class_2596;)V
    @Inject(
        method = {
            "send(Lnet/minecraft/network/packet/Packet;)V",
            "method_10743(Lnet/minecraft/class_2596;)V"
        },
        at = @At("HEAD"),
        remap = false,
        require = 0
    )
    private void onSendPacket(@Coerce Object packet, CallbackInfo ci) {
        if (!AutomationManager.isBuffEnabled() || packet == null) {
            return;
        }

        try {
            Object client = CommandDispatcher.getClient();
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            boolean isBreaking = false;
            if (im != null) {
                try { isBreaking = (boolean) MappingHelper.invokeMethod(im, "isBreakingBlock"); } catch (Throwable ignored) {}
            }

            // ONLY modify onGround = true during active block breaking
            if (isBreaking) {
                String pName = packet.getClass().getName();
                if (pName.contains("PlayerMoveC2SPacket") || pName.contains("class_2828")) {
                    Class<?> curr = packet.getClass();
                    while (curr != null && curr != Object.class) {
                        for (Field f : curr.getDeclaredFields()) {
                            String fn = f.getName();
                            if (fn.equals("onGround") || fn.equals("field_12891") || fn.equals("field_29179")) {
                                f.setAccessible(true);
                                f.setBoolean(packet, true);
                                break;
                            }
                        }
                        curr = curr.getSuperclass();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
