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
            long dayTime = -2; // 使用 -2 作为未初始化的标志，因为 -1 在 dayTime 中有意义 (冻结时间)

            // 策略 1: 属性读取 (Record or Class)
            try { gameTime = ((Number) MappingHelper.invokeMethod(packet, "gameTime")).longValue(); } catch (Exception ignored) {}
            try { dayTime = ((Number) MappingHelper.invokeMethod(packet, "dayTime")).longValue(); } catch (Exception ignored) {}

            if (gameTime == -1) {
                try { gameTime = ((Number) MappingHelper.invokeMethod(packet, "method_11871")).longValue(); } catch (Exception ignored) {}
            }
            if (dayTime == -2) {
                try { dayTime = ((Number) MappingHelper.invokeMethod(packet, "method_11870")).longValue(); } catch (Exception ignored) {}
            }

            // 策略 2: 暴力查找 long 字段 (WorldTimeUpdateS2CPacket 通常有两个 long 字段: gameTime, dayTime)
            if (gameTime == -1 || dayTime == -2) {
                java.util.List<Long> longFields = new java.util.ArrayList<>();
                // 如果是 Record 类型，优先遍历 RecordComponents (1.21.2+)
                if (packet.getClass().isRecord()) {
                    for (java.lang.reflect.RecordComponent rc : packet.getClass().getRecordComponents()) {
                        if (rc.getType() == long.class) {
                            try { longFields.add(((Number) rc.getAccessor().invoke(packet)).longValue()); } catch (Exception ignored) {}
                        }
                    }
                }

                // 无论是否是 Record，都扫描字段作为兜底
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
                // 容错：如果只拿到了一个，至少同步一个
                long finalGame = gameTime != -1 ? gameTime : PerformanceMonitor.getLastGameTime();
                long finalDay = dayTime != -2 ? dayTime : PerformanceMonitor.getLastDayTime();
                if (finalGame != -1 && finalDay != -2) {
                    PerformanceMonitor.onWorldTimeUpdate(finalGame, finalDay);
                }
            }
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
                // 1.21.1 fallback: field_3696
                try {
                    dispatcher = MappingHelper.getFieldValue(handler, "field_3696", null);
                } catch (Exception ignored1) {
                    // 1.21.4+ fallback: field_3691
                    try {
                        dispatcher = MappingHelper.getFieldValue(handler, "field_3691", null);
                    } catch (Exception ignored2) {}
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

            // 注册所有子命令支持自动补全和输入合法性
            String[] subcommands = {
                "time", "hp", "xp", "tune", "tps", "list", "choose", "cs",
                "slot", "tools", "drop", "attack", "atk", "use", "luse",
                "respawn", "buff", "look", "status", "stop", "debug", "mapping"
            };

            for (String sub : subcommands) {
                Object subBuilder = literalMethod.invoke(null, sub);
                if ("respawn".equals(sub) || "buff".equals(sub)) {
                    // 为 respawn 和 buff 专门添加 "on" 和 "off" 的自动补全子项
                    Object onBuilder = literalMethod.invoke(null, "on");
                    Object offBuilder = literalMethod.invoke(null, "off");
                    thenMethod.invoke(subBuilder, onBuilder);
                    thenMethod.invoke(subBuilder, offBuilder);
                }
                Object subArgBuilder = argumentMethod.invoke(null, "args", greedyType);
                thenMethod.invoke(subBuilder, subArgBuilder);
                thenMethod.invoke(mccBuilder, subBuilder);
            }

            // 添加直接挂在/mcc下的贪婪兜底，确保 /mcc 后面带任意未知参数时也保持合法性
            Object fallbackArgBuilder = argumentMethod.invoke(null, "args", greedyType);
            thenMethod.invoke(mccBuilder, fallbackArgBuilder);

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

    @Inject(method = {"sendPacket(Lnet/minecraft/network/packet/Packet;)V", "method_52787"}, at = @At("HEAD"), remap = false, require = 0)
    private void onSendPacket(@Coerce Object packet, CallbackInfo ci) {
        if (!net.mcc.AutomationManager.isBuffEnabled() || packet == null) {
            return;
        }

        try {
            String className = packet.getClass().getName();
            // 1. 移动数据包（PlayerMoveC2SPacket 及子类）：挖掘时伪装 onGround = true，消除服务端的 5 倍飞行减速惩罚
            if (className.contains("PlayerMoveC2SPacket") || className.contains("class_2828")) {
                Object client = CommandDispatcher.getClient();
                if (client != null) {
                    Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                    if (im != null) {
                        Boolean isBreaking = (Boolean) MappingHelper.invokeMethod(im, "isBreakingBlock");
                        if (Boolean.TRUE.equals(isBreaking)) {
                            try { MappingHelper.setFieldValue(packet, "onGround", true); } catch (Exception ignored) {}
                            try { MappingHelper.setFieldValue(packet, "field_12891", true); } catch (Exception ignored) {}
                        }
                    }
                }
            }

            // 2. 挖掘数据包 (PlayerActionC2SPacket) 进度同步
            if (className.contains("PlayerActionC2SPacket") || className.contains("class_2846")) {
                Object action = null;
                try {
                    action = MappingHelper.invokeMethod(packet, "getAction");
                } catch (Exception e) {
                    try {
                        action = MappingHelper.invokeMethod(packet, "method_12363");
                    } catch (Exception ignored) {}
                }

                if (action != null) {
                    String actionStr = String.valueOf(action);
                    if (actionStr.contains("UPDATE") || actionStr.contains("START")) {
                        Object client = CommandDispatcher.getClient();
                        if (client != null) {
                            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                            Object player = CommandDispatcher.getClientPlayer();
                            Object world = CommandDispatcher.getClientWorld();
                            if (im != null && player != null && world != null) {
                                Object pos = MappingHelper.invokeMethod(packet, "getPos");
                                if (pos != null) {
                                    Object blockState = MappingHelper.invokeMethod(world, "getBlockState", pos);
                                    if (blockState != null) {
                                        float delta = ((Number) MappingHelper.invokeMethod(blockState, "calcBlockBreakingDelta", player, world, pos)).floatValue();
                                        if (delta > 0.0f) {
                                            Float currProgress = (Float) MappingHelper.getFieldValue(im, "currentBreakingProgress", null);
                                            if (currProgress != null && currProgress < delta) {
                                                MappingHelper.setFieldValue(im, "currentBreakingProgress", delta);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
