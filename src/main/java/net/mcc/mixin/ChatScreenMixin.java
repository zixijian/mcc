package net.mcc.mixin;

import net.mcc.CommandDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.screens.ChatScreen")
public class ChatScreenMixin {

    @Inject(method = {"handleChatInput(Ljava/lang/String;Z)Z", "method_2108(Ljava/lang/String;Z)Z"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onHandleChatInputWithHistory(String message, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (message != null && message.startsWith("/mcc")) {
            if (CommandDispatcher.dispatch(message)) {
                if (addToHistory) {
                    addRecentChatToGui(message);
                }
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = {"handleChatInput(Ljava/lang/String;)Z", "method_2108(Ljava/lang/String;)Z"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onHandleChatInputSingle(String message, CallbackInfoReturnable<Boolean> cir) {
        if (message != null && message.startsWith("/mcc")) {
            if (CommandDispatcher.dispatch(message)) {
                addRecentChatToGui(message);
                cir.setReturnValue(true);
            }
        }
    }

    private void addRecentChatToGui(String message) {
        try {
            Object client = CommandDispatcher.getClient();
            if (client != null) {
                Object gui = net.mcc.MappingHelper.getFieldValue(client, "gui", null);
                if (gui == null) gui = net.mcc.MappingHelper.getFieldValue(client, "ingameGUI", null);
                if (gui != null) {
                    Object chatHud = net.mcc.MappingHelper.invokeMethod(gui, "getChat");
                    if (chatHud != null) {
                        try {
                            net.mcc.MappingHelper.invokeMethod(chatHud, "addRecentChat", message);
                        } catch (Exception e1) {
                            try {
                                net.mcc.MappingHelper.invokeMethod(chatHud, "method_1812", message);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
