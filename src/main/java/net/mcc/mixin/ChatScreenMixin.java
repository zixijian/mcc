package net.mcc.mixin;

import net.mcc.CommandDispatcher;
import net.mcc.MappingHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.screens.ChatScreen")
public class ChatScreenMixin {

    @Inject(method = {"handleChatInput(Ljava/lang/String;Z)Z", "method_2108(Ljava/lang/String;Z)Z"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onHandleChatInputWithHistory(String message, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (message != null && message.startsWith("/mcc")) {
            if (addToHistory) {
                addToRecentHistory(message);
            }
            if (CommandDispatcher.dispatch(message)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = {"handleChatInput(Ljava/lang/String;)Z", "method_2108(Ljava/lang/String;)Z"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onHandleChatInputSingle(String message, CallbackInfoReturnable<Boolean> cir) {
        if (message != null && message.startsWith("/mcc")) {
            addToRecentHistory(message);
            if (CommandDispatcher.dispatch(message)) {
                cir.setReturnValue(true);
            }
        }
    }

    private void addToRecentHistory(String message) {
        try {
            Object client = CommandDispatcher.getClient();
            if (client == null) return;
            Object gui = null;
            try { gui = MappingHelper.getFieldValue(client, "gui", null); } catch (Exception ignored) {}
            if (gui == null) {
                try { gui = MappingHelper.getFieldValue(client, "ingameGUI", null); } catch (Exception ignored) {}
            }
            if (gui != null) {
                Object chat = null;
                try { chat = MappingHelper.getFieldValue(gui, "chat", null); } catch (Exception ignored) {}
                if (chat == null) {
                    try { chat = MappingHelper.getFieldValue(gui, "chatHud", null); } catch (Exception ignored) {}
                }
                if (chat != null) {
                    try {
                        MappingHelper.invokeMethod(chat, "addRecentChat", message);
                    } catch (Exception e1) {
                        try { MappingHelper.invokeMethod(chat, "method_1812", message); } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
