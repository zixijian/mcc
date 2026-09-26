package net.mcc.mixin;

import net.mcc.CommandDispatcher;
import net.mcc.MappingHelper;
import net.mcc.PerformanceMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.lang.reflect.Method;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientPacketListener")
public class ClientPlayNetworkHandlerMixin {

    // onWorldTimeUpdate
    @Inject(method = {"handleSetTime", "onWorldTimeUpdate", "method_11079"}, at = @At("HEAD"), remap = false, require = 0)
    private void onWorldTimeUpdate(@Coerce Object packet, CallbackInfo ci) {
        try {
            long gameTime = -1;
            long dayTime = -2;

            try { gameTime = ((Number) MappingHelper.invokeMethod(packet, "gameTime")).longValue(); } catch (Exception ignored) {}
            try { dayTime = ((Number) MappingHelper.invokeMethod(packet, "dayTime")).longValue(); } catch (Exception ignored) {}

            if (gameTime == -1) {
                try { gameTime = ((Number) MappingHelper.invokeMethod(packet, "getGameTime")).longValue(); } catch (Exception ignored) {}
            }
            if (dayTime == -2) {
                try { dayTime = ((Number) MappingHelper.invokeMethod(packet, "getDayTime")).longValue(); } catch (Exception ignored) {}
            }

            if (gameTime == -1 || dayTime == -2) {
                java.util.List<Long> longFields = new java.util.ArrayList<>();
                if (packet.getClass().isRecord()) {
                    for (java.lang.reflect.RecordComponent rc : packet.getClass().getRecordComponents()) {
                        if (rc.getType() == long.class) {
                            try { longFields.add(((Number) rc.getAccessor().invoke(packet)).longValue()); } catch (Exception ignored) {}
                        }
                    }
                }

                if (longFields.size() < 2) {
                    for (java.lang.reflect.Field f : packet.getClass().getDeclaredFields()) {
                        if (f.getType() == long.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                            try {
                                f.setAccessible(true);
                                longFields.add(f.getLong(packet));
                            } catch (Exception ignored) {}
                        }
                    }
                }

                if (longFields.size() >= 2) {
                    if (gameTime == -1) gameTime = longFields.get(0);
                    if (dayTime == -2) dayTime = longFields.get(1);
                }
            }

            if (gameTime != -1 && dayTime != -2) {
                PerformanceMonitor.onWorldTimeUpdate(gameTime, dayTime);
            } else if (gameTime != -1 || dayTime != -2) {
                long finalGame = gameTime != -1 ? gameTime : PerformanceMonitor.getLastGameTime();
                long finalDay = dayTime != -2 ? dayTime : PerformanceMonitor.getLastDayTime();
                if (finalGame != -1 && finalDay != -2) {
                    PerformanceMonitor.onWorldTimeUpdate(finalGame, finalDay);
                }
            }
        } catch (Exception e) {}
    }

    // handleCommands / onCommandTree
    @Inject(method = {"handleCommands", "onCommandTree", "method_11100", "method_11145", "method_64361"}, at = @At("TAIL"), remap = false, require = 0)
    private void onCommandTree(@Coerce Object packet, CallbackInfo ci) {
        injectMccNode(this);
    }

    // getCustomSuggestions / getChatSuggestions
    @Inject(method = {"getCustomSuggestions", "getChatSuggestions", "method_9259", "method_63852"}, at = @At("HEAD"), remap = false, require = 0)
    private void onGetChatSuggestions(CallbackInfo ci) {
        injectMccNode(this);
    }

    private static void injectMccNode(Object handler) {
        try {
            Object dispatcher = null;
            try {
                dispatcher = MappingHelper.invokeMethod(handler, "getCommandDispatcher");
            } catch (Exception e) {
                try {
                    dispatcher = MappingHelper.getFieldValue(handler, "commands", null);
                } catch (Exception ignored1) {
                    try {
                        dispatcher = MappingHelper.getFieldValue(handler, "field_3696", null);
                    } catch (Exception ignored2) {
                        try {
                            dispatcher = MappingHelper.getFieldValue(handler, "field_3691", null);
                        } catch (Exception ignored3) {}
                    }
                }
            }
            if (dispatcher == null) return;

            Class<?> literalBuilderClass = Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");
            Class<?> requiredBuilderClass = Class.forName("com.mojang.brigadier.builder.RequiredArgumentBuilder");
            Class<?> stringArgClass = Class.forName("com.mojang.brigadier.arguments.StringArgumentType");
            Class<?> argumentBuilderClass = Class.forName("com.mojang.brigadier.builder.ArgumentBuilder");

            Method literalMethod = literalBuilderClass.getMethod("literal", String.class);
            Method argumentMethod = requiredBuilderClass.getMethod("argument", String.class, Class.forName("com.mojang.brigadier.arguments.ArgumentType"));
            Method greedyMethod = stringArgClass.getMethod("greedyString");
            Method thenMethod = argumentBuilderClass.getMethod("then", argumentBuilderClass);
            Method buildMethod = argumentBuilderClass.getMethod("build");

            Object mccBuilder = literalMethod.invoke(null, "mcc");
            Object greedyType = greedyMethod.invoke(null);

            String[] subcommands = {
                "time", "hp", "xp", "tune", "tps", "list", "choose", "cs",
                "slot", "tools", "drop", "attack", "atk", "use", "luse",
                "respawn", "look", "status", "stop", "debug", "mapping"
            };

            for (String sub : subcommands) {
                Object subBuilder = literalMethod.invoke(null, sub);
                if ("respawn".equals(sub)) {
                    Object onBuilder = literalMethod.invoke(null, "on");
                    Object offBuilder = literalMethod.invoke(null, "off");
                    thenMethod.invoke(subBuilder, onBuilder);
                    thenMethod.invoke(subBuilder, offBuilder);
                }
                Object subArgBuilder = argumentMethod.invoke(null, "args", greedyType);
                thenMethod.invoke(subBuilder, subArgBuilder);
                thenMethod.invoke(mccBuilder, subBuilder);
            }

            Object fallbackArgBuilder = argumentMethod.invoke(null, "args", greedyType);
            thenMethod.invoke(mccBuilder, fallbackArgBuilder);

            Object mccNode = buildMethod.invoke(mccBuilder);

            Object root = MappingHelper.invokeMethod(dispatcher, "getRoot");
            MappingHelper.invokeMethod(root, "addChild", mccNode);

        } catch (Exception e) {}
    }

    @Inject(method = {"sendCommand(Ljava/lang/String;)V", "method_45730"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendCommandVoid(String command, CallbackInfo ci) {
        if (CommandDispatcher.dispatch("/" + command)) {
            ci.cancel();
        }
    }

    @Inject(method = {"sendCommand(Ljava/lang/String;)Z"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendCommandBoolean(String command, CallbackInfoReturnable<Boolean> cir) {
        if (CommandDispatcher.dispatch("/" + command)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = {"sendChatCommand", "sendUnsignedCommand", "method_45729"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onSendChatCommand(String command, CallbackInfo ci) {
        if (CommandDispatcher.dispatch("/" + command)) {
            ci.cancel();
        }
    }
}
