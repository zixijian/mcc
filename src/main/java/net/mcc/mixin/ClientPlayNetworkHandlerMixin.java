package net.mcc.mixin;

import net.mcc.CommandDispatcher;
import net.mcc.MappingHelper;
import net.mcc.PerformanceMonitor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.lang.reflect.Method;

@Mixin(targets = "net.minecraft.class_634") // ClientPlayNetworkHandler
public class ClientPlayNetworkHandlerMixin {

    // onWorldTimeUpdate
    @Inject(method = "method_11079", at = @At("HEAD"), remap = false, require = 0)
    private void onWorldTimeUpdate(@Coerce Object packet, CallbackInfo ci) {
        try {
            long gameTime = -1;
            long dayTime = -1;
            // 策略 1: 属性读取 (Record or Class)
            try { gameTime = ((Number) MappingHelper.invokeMethod(packet, "gameTime")).longValue(); } catch (Exception ignored) {}
            try { dayTime = ((Number) MappingHelper.invokeMethod(packet, "dayTime")).longValue(); } catch (Exception ignored) {}

            if (gameTime == -1) {
                try { gameTime = ((Number) MappingHelper.invokeMethod(packet, "method_11871")).longValue(); } catch (Exception ignored) {}
            }
            if (dayTime == -1) {
                try { dayTime = ((Number) MappingHelper.invokeMethod(packet, "method_11870")).longValue(); } catch (Exception ignored) {}
            }

            // 策略 2: 暴力查找 long 字段 (WorldTimeUpdateS2CPacket 通常有两个 long 字段)
            if (gameTime == -1 || dayTime == -1) {
                int count = 0;
                for (java.lang.reflect.Field f : packet.getClass().getDeclaredFields()) {
                    if (f.getType() == long.class) {
                        f.setAccessible(true);
                        long val = f.getLong(packet);
                        if (count == 0) gameTime = val;
                        else if (count == 1) dayTime = val;
                        count++;
                    }
                }
            }
            if (gameTime != -1) PerformanceMonitor.onWorldTimeUpdate(gameTime, dayTime != -1 ? dayTime : gameTime);
        } catch (Exception e) {}
    }

    // onCommandTree (1.21.1 - method_11100)
    @Inject(method = "method_11100", at = @At("TAIL"), remap = false, require = 0)
    private void onCommandTree1211(@Coerce Object packet, CallbackInfo ci) {
        injectMccNode(this);
    }

    // onCommandTree (1.21.2 - method_11100 with different descriptor)
    @Inject(method = "method_11100(Lnet/minecraft/class_2633;)V", at = @At("TAIL"), remap = false, require = 0)
    private void onCommandTree1212(@Coerce Object packet, CallbackInfo ci) {
        injectMccNode(this);
    }

    // onCommandTree (1.21.4 - method_11145)
    @Inject(method = "method_11145", at = @At("TAIL"), remap = false, require = 0)
    private void onCommandTree1214(@Coerce Object packet, CallbackInfo ci) {
        injectMccNode(this);
    }

    // onCommandTree (1.21.4+ - method_64361)
    @Inject(method = "method_64361", at = @At("TAIL"), remap = false, require = 0)
    private void onCommandTree1214Plus(@Coerce Object packet, CallbackInfo ci) {
        injectMccNode(this);
    }

    // 针对 1.21.1/1.21.4+ 的 getChatSuggestions (method_9259)
    @Inject(method = "method_9259", at = @At("TAIL"), remap = false, require = 0)
    private void onGetChatSuggestions(CallbackInfo ci) {
        injectMccNode(this);
    }

    // 拦截 ClientCommandSource 相关的建议请求
    @Inject(method = "method_9259", at = @At("HEAD"), remap = false, require = 0)
    private void onGetChatSuggestionsPre(CallbackInfo ci) {
        injectMccNode(this);
    }

    // getChatSuggestions (1.21.4+ - method_63852)
    @Inject(method = "method_63852", at = @At("HEAD"), remap = false, require = 0)
    private void onGetChatSuggestions1214Plus(CallbackInfo ci) {
        injectMccNode(this);
    }

    private static void injectMccNode(Object handler) {
        try {
            Object dispatcher = null;
            try {
                dispatcher = MappingHelper.invokeMethod(handler, "getCommandDispatcher");
            } catch (Exception e) {
                // 1.21.4+ fallback: field_3691
                try {
                    dispatcher = MappingHelper.getFieldValue(handler, "field_3691", null);
                } catch (Exception ignored) {}
            }
            if (dispatcher == null) return;

            Class<?> literalBuilderClass = Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");
            Class<?> requiredBuilderClass = Class.forName("com.mojang.brigadier.builder.RequiredArgumentBuilder");
            Class<?> stringArgClass = Class.forName("com.mojang.brigadier.arguments.StringArgumentType");
            Class<?> argumentBuilderClass = Class.forName("com.mojang.brigadier.builder.ArgumentBuilder");

            Method literalMethod = literalBuilderClass.getMethod("literal", String.class);
            Method argumentMethod = requiredBuilderClass.getMethod("argument", String.class, Class.forName("com.mojang.brigadier.arguments.ArgumentType"));
            Method greedyMethod = stringArgClass.getMethod("greedyString");
            Method thenMethod = literalBuilderClass.getMethod("then", argumentBuilderClass);
            Method buildMethod = literalBuilderClass.getMethod("build");

            Object mccBuilder = literalMethod.invoke(null, "mcc");
            Object greedyType = greedyMethod.invoke(null);
            Object argBuilder = argumentMethod.invoke(null, "args", greedyType);

            thenMethod.invoke(mccBuilder, argBuilder);
            Object mccNode = buildMethod.invoke(mccBuilder);

            Object root = MappingHelper.invokeMethod(dispatcher, "getRoot");
            MappingHelper.invokeMethod(root, "addChild", mccNode);

        } catch (Exception e) {}
    }

    @Inject(method = "method_45730(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0) // sendCommand
    private void onSendCommand(String command, CallbackInfo ci) {
        if (CommandDispatcher.dispatch("/" + command)) {
            ci.cancel();
        }
    }

    @Inject(method = "method_45729(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0) // sendChatCommand
    private void onSendChatCommand(String command, CallbackInfo ci) {
        if (CommandDispatcher.dispatch("/" + command)) {
            ci.cancel();
        }
    }
}
