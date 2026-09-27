package net.mcc;

import java.lang.reflect.Field;

public class AutomationManager {
    private static int attackFreq = -1;
    private static int useFreq = -1;
    private static int attackTimer = 0;
    private static int useTimer = 0;
    private static boolean attackOnce = false;
    private static boolean useOnce = false;
    private static boolean autoRespawn = false;
    private static boolean singleRespawnTriggered = false;

    private static int internalAttackTicks = 0;
    private static int waitTicksAfterHalfCharge = -1;

    private static Float lockedPitch = null;
    private static Float lockedYaw = null;

    private static int luseCount = -2; // -2 for inactive, -1 for infinite (0), >= 1 for positive counts
    private static int luseStage = 0;
    private static int luseDelayTicks = 0;
    private static int luseActiveTicks = 0;
    private static boolean luseStarted = false;
    private static long lastProcessedGameTime = -1;
    private static int clientTickCount = 0;
    private static int lastProcessedTickCount = -1;

    public static void incrementClientTick() {
        clientTickCount++;
    }

    private static int lastLuseSlot = -1;
    private static Object lastLuseItem = null;
    private static int lastLuseCount = -1;

    public static void setLuse(int count) {
        luseCount = count;
        luseStage = 0;
        luseDelayTicks = 0;
        luseActiveTicks = 0;
        luseStarted = false;
        lastLuseSlot = -1;
        lastLuseItem = null;
        lastLuseCount = -1;
        if (count != -2) {
            useFreq = -1;
            attackFreq = -1;
            try {
                Object client = CommandDispatcher.getClient();
                releaseKeyTranslation(client, "key.attack");
            } catch (Exception ignored) {}
            if (count == -1) {
                CommandDispatcher.addFeedback("§a启动持续长按使用");
            } else {
                CommandDispatcher.addFeedback("§a启动长按使用: " + count + " 次");
            }
        } else {
            try {
                Object client = CommandDispatcher.getClient();
                releaseKeyTranslation(client, "key.use");
            } catch (Exception ignored) {}
            CommandDispatcher.addFeedback("§e已停止长按使用");
        }
    }

    public static void setAttack(int freq, boolean hasArgs) {
        if (!hasArgs) {
            attackOnce = true;
            attackTimer = 0; // 重置计时器
            internalAttackTicks = 100; // 让单次点击能立即进入延迟判定
            CommandDispatcher.addFeedback("§a执行攻击一次");
            return;
        }
        attackFreq = freq;
        attackTimer = 0;
        if (freq >= 0) {
            useFreq = -1; // 如果开启攻击，关闭自动使用
            try {
                Object client = CommandDispatcher.getClient();
                releaseKeyTranslation(client, "key.use");
            } catch (Exception ignored) {}
        } else {
            // 如果是关闭攻击，确保按键被释放
            try {
                Object client = CommandDispatcher.getClient();
                releaseKeyTranslation(client, "key.attack");
            } catch (Exception ignored) {}
        }
        CommandDispatcher.addFeedback("§a自动攻击: " + (freq == -1 ? "关闭" : (freq == 0 ? "持续" : freq + " ticks")));
    }

    public static void setUse(int freq, boolean hasArgs) {
        if (!hasArgs) {
            useOnce = true;
            useTimer = 0; // 重置计时器
            CommandDispatcher.addFeedback("§a执行使用一次");
            return;
        }
        useFreq = freq;
        useTimer = 0;
        if (freq >= 0) {
            attackFreq = -1; // 如果开启使用，关闭自动攻击
            try {
                Object client = CommandDispatcher.getClient();
                releaseKeyTranslation(client, "key.attack");
            } catch (Exception ignored) {}
        } else {
            // 如果是关闭使用，确保按键被释放
            try {
                Object client = CommandDispatcher.getClient();
                releaseKeyTranslation(client, "key.use");
            } catch (Exception ignored) {}
        }
        CommandDispatcher.addFeedback("§a自动使用: " + (freq == -1 ? "关闭" : (freq == 0 ? "持续" : freq + " ticks")));
    }

    public static void handleRespawnCommand(String arg) {
        if ("on".equals(arg)) {
            autoRespawn = true;
            CommandDispatcher.addFeedback("§a自动复活: 开启");
            return;
        } else if ("off".equals(arg)) {
            autoRespawn = false;
            CommandDispatcher.addFeedback("§a自动复活: 关闭");
            return;
        }

        // 不带参数（或未知参数）则执行单次复活
        try {
            Object player = CommandDispatcher.getClientPlayer();
            if (player != null) {
                float hp = ((Number) MappingHelper.invokeMethod(player, "getHealth")).floatValue();
                if (hp <= 0) {
                    singleRespawnTriggered = true;
                    CommandDispatcher.addFeedback("§a已触发单次复活");
                    return;
                } else {
                    CommandDispatcher.addFeedback("§e玩家当前处于存活状态，无需复活");
                    return;
                }
            }
        } catch (Exception ignored) {}
        CommandDispatcher.addFeedback("§c无法获取玩家状态");
    }

    public static void setLook(float pitch, float yaw) throws Exception {
        lockedPitch = pitch;
        lockedYaw = yaw;
        Object player = CommandDispatcher.getClientPlayer();
        if (player != null) {
            MappingHelper.invokeMethod(player, "setYaw", yaw);
            MappingHelper.invokeMethod(player, "setPitch", pitch);
            // 同时更新渲染和之前的旋转角度以防抖动
            try { MappingHelper.setFieldValue(player, "field_6031", yaw); } catch (Exception ignored) {}
            try { MappingHelper.setFieldValue(player, "field_6004", pitch); } catch (Exception ignored) {}
            try { MappingHelper.setFieldValue(player, "field_5965", yaw); } catch (Exception ignored) {} // prevYaw
            try { MappingHelper.setFieldValue(player, "field_6014", pitch); } catch (Exception ignored) {} // prevPitch
            CommandDispatcher.addFeedback(String.format("§a设定朝向: Pitch %.1f, Yaw %.1f", pitch, yaw));
        }
    }

    public static void showLook() throws Exception {
        Object player = CommandDispatcher.getClientPlayer();
        if (player != null) {
            float yaw = ((Number) MappingHelper.invokeMethod(player, "getYaw")).floatValue();
            float pitch = ((Number) MappingHelper.invokeMethod(player, "getPitch")).floatValue();
            CommandDispatcher.addFeedback(String.format("§e当前朝向: Pitch %.1f, Yaw %.1f", pitch, yaw));
        } else {
            CommandDispatcher.addFeedback("§c无法获取玩家朝向");
        }
    }

    public static void stopAll() {
        attackFreq = -1; useFreq = -1;
        attackOnce = useOnce = false;
        lockedPitch = lockedYaw = null;
        luseCount = -2;
        luseStage = 0;
        luseDelayTicks = 0;
        luseActiveTicks = 0;
        luseStarted = false;
        try {
            Object client = CommandDispatcher.getClient();
            releaseKeyTranslation(client, "key.attack");
            releaseKeyTranslation(client, "key.use");
        } catch (Exception e) {}
        CommandDispatcher.addFeedback("§e已停止所有自动行为");
    }

    public static void showStatus() {
        String luseStr = luseCount == -2 ? "关闭" : (luseCount == -1 ? "持续" : luseCount + " 次");
        CommandDispatcher.addFeedback(String.format("§b[MCC] Atk:%d Use:%d Luse:%s Rsp:%b", attackFreq, useFreq, luseStr, autoRespawn));
    }

    public static void probeMappings() {
        CommandDispatcher.addFeedback("§d[MCC Mapping Probe]");
        try {
            Class<?> mcClass = MappingHelper.getClass("MinecraftClient");
            try {
                Field f = mcClass.getDeclaredField("field_1755");
                CommandDispatcher.addFeedback("§7- Version detect: field_1755 type is " + f.getType().getSimpleName());
            } catch (Exception ignored) {}

            Object client = CommandDispatcher.getClient();
            Object player = CommandDispatcher.getClientPlayer();

            // 探测 Screen
            try {
                Class<?> screenClass = MappingHelper.getClass("Screen");
                Object screen = MappingHelper.findUniqueFieldByType(client, screenClass);
                CommandDispatcher.addFeedback("§7- Screen: " + (screen == null ? "None" : screen.getClass().getSimpleName()));
            } catch (Exception e) { CommandDispatcher.addFeedback("§c- Screen lookup failed: " + e.getMessage()); }

            // 探测 Cooldown 字段
            String[] atkCooldowns = {"field_1752", "field1752", "field_1755", "field1755", "attackCooldown"};
            for (String f : atkCooldowns) {
                try {
                    Object val = MappingHelper.getFieldValue(client, f, null);
                    CommandDispatcher.addFeedback("§a- AtkCooldown field " + f + " found, value: " + val);
                } catch (Exception ignored) {}
            }

            // 探测 doItemUse 方法
            String[] useMethods = {"method_1531", "method1531", "doItemUse"};
            for (String m : useMethods) {
                try {
                    MappingHelper.findMethod(client.getClass(), m);
                    CommandDispatcher.addFeedback("§a- doItemUse method " + m + " found");
                } catch (Exception ignored) {}
            }

            // 探测 interactItem / interactBlock
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            if (im != null) {
                String[] imMethods = {"method_2896", "method_2919", "method_2902", "interactItem", "interactBlock"};
                for (String m : imMethods) {
                    try {
                        for (java.lang.reflect.Method jm : im.getClass().getDeclaredMethods()) {
                            if (jm.getName().equals(m) || jm.getName().equals(m.replace("_", ""))) {
                                CommandDispatcher.addFeedback("§a- InteractionManager method " + m + " found with " + jm.getParameterCount() + " params");
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            CommandDispatcher.addFeedback("§cProbe failed: " + e.toString());
        }
    }

    private static class LuseStackInfo {
        int slot;
        Object item;
        int count;
        boolean isEmpty;

        LuseStackInfo(int slot, Object item, int count, boolean isEmpty) {
            this.slot = slot;
            this.item = item;
            this.count = count;
            this.isEmpty = isEmpty;
        }
    }

    private static LuseStackInfo getLuseStackInfo(Object player) {
        try {
            Object inv = MappingHelper.getFieldValue(player, "inventory", null);
            if (inv != null) {
                int selectedSlot = ((Number) MappingHelper.getFieldValue(inv, "selectedSlot", null)).intValue();
                Object main = MappingHelper.getFieldValue(inv, "main", null);
                if (main instanceof java.util.List) {
                    Object stack = ((java.util.List<?>) main).get(selectedSlot);
                    if (stack != null) {
                        boolean isEmpty = (boolean) MappingHelper.invokeMethod(stack, "isEmpty");
                        if (isEmpty) {
                            return new LuseStackInfo(selectedSlot, null, 0, true);
                        } else {
                            Object item = MappingHelper.invokeMethod(stack, "getItem");
                            int count = ((Number) MappingHelper.invokeMethod(stack, "getCount")).intValue();
                            return new LuseStackInfo(selectedSlot, item, count, false);
                        }
                    }
                }
                return new LuseStackInfo(selectedSlot, null, 0, true);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isLuseStackChanged(Object player) {
        if (lastLuseSlot == -1) return true; // No consumable recorded or not a consumable
        LuseStackInfo current = getLuseStackInfo(player);
        if (current == null) return true;

        if (current.slot != lastLuseSlot) {
            return true; // Slot changed
        }
        if (current.isEmpty) {
            return true; // Item was consumed and stack became empty
        }
        if (current.item != lastLuseItem) {
            return true; // Item changed (e.g., potion bottle turned into glass bottle)
        }
        if (current.count != lastLuseCount) {
            return true; // Count changed (decremented)
        }
        return false;
    }

    /**
     * 客户端 Tick 回调
     */
    public static void onClientTick() {
        if (clientTickCount == lastProcessedTickCount) {
            return;
        }
        lastProcessedTickCount = clientTickCount;

        try {
            Object client = CommandDispatcher.getClient();
            Object player = CommandDispatcher.getClientPlayer();
            if (player == null) return;

            // 1. 自动复活 (无视屏幕/GUI打开状态，优先处理)
            try {
                float hp = ((Number) MappingHelper.invokeMethod(player, "getHealth")).floatValue();
                if (hp <= 0) {
                    if (autoRespawn || singleRespawnTriggered) {
                        MappingHelper.invokeMethod(player, "requestRespawn");
                        singleRespawnTriggered = false;
                    }
                } else {
                    singleRespawnTriggered = false; // 活着的时候重置，防止意外
                }
            } catch (Exception ignored) {}

            // 健壮的当前屏幕检测：基于类型查找，防止 Intermediary 偏移导致误判
            Object currentScreen = null;
            try {
                Class<?> screenClass = MappingHelper.getClass("Screen");
                currentScreen = MappingHelper.findUniqueFieldByType(client, screenClass);
            } catch (Exception ignored) {}
            if (currentScreen != null) return;

            // 视角锁定
            if (lockedPitch != null && lockedYaw != null) {
                try {
                    MappingHelper.invokeMethod(player, "setYaw", lockedYaw);
                    MappingHelper.invokeMethod(player, "setPitch", lockedPitch);
                    // 同时更新渲染角度以防抖动
                    try { MappingHelper.setFieldValue(player, "field_6031", lockedYaw); } catch (Exception ignored) {}
                    try { MappingHelper.setFieldValue(player, "field_6004", lockedPitch); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            }

            // 2. 攻击逻辑
            // 强制蓄力条始终显示为满值 (UI 表现)
            try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}

            if (attackOnce) {
                internalAttackTicks++;
                // 单次攻击判定逻辑：遵循 1.0f + 1 tick 延迟
                float progress = 0f;
                try {
                    float ppt = ((Number) MappingHelper.invokeMethod(player, "getAttackCooldownProgressPerTick")).floatValue();
                    progress = internalAttackTicks * ppt;
                    float nativeProgress = ((Number) MappingHelper.invokeMethod(player, "getAttackCooldownProgress", 0.0f)).floatValue();
                    if (nativeProgress > progress) progress = nativeProgress;
                } catch (Exception e) { progress = internalAttackTicks * 0.1f; }

                if (waitTicksAfterHalfCharge == -1 && progress >= 1.0f) {
                    waitTicksAfterHalfCharge = 1;
                }

                if (waitTicksAfterHalfCharge > 0) {
                    waitTicksAfterHalfCharge--;
                } else if (waitTicksAfterHalfCharge == 0) {
                    resetAttackCooldown(client);
                    triggerAttack(client, player);
                    internalAttackTicks = 0;
                    waitTicksAfterHalfCharge = -1;
                    attackOnce = false;
                }
            } else if (attackFreq == 0) {
                // 持续攻击：还原主分支逻辑，不加内部延迟
                resetAttackCooldown(client);
                triggerAttack(client, player);
                internalAttackTicks = 0;
            } else if (attackFreq > 0) {
                // 频率攻击：还原主分支逻辑
                if (--attackTimer <= 0) {
                    resetAttackCooldown(client);
                    triggerAttack(client, player);
                    attackTimer = attackFreq;
                    internalAttackTicks = 0;
                }
            } else {
                attackTimer = 0;
                internalAttackTicks = 0;
                waitTicksAfterHalfCharge = -1;
            }

            // 3. 使用逻辑
            if (useOnce) {
                resetUseCooldown(client);
                triggerItemUse(client, player);
                useOnce = false;
            } else if (useFreq == 0) {
                resetUseCooldown(client);
                pressKeyTranslation(client, "key.use");
                // 持续按住模式下，如果当前没有在“使用”（如吃东西、拉弓），则尝试触发
                boolean isUsing = false;
                try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}
                if (!isUsing) {
                    triggerItemUse(client, player);
                }
            } else if (useFreq > 0) {
                releaseKeyTranslation(client, "key.use");
                if (--useTimer <= 0) {
                    resetUseCooldown(client);
                    triggerItemUse(client, player);
                    useTimer = useFreq;
                }
            }

            // 4. Luse (长按使用) 逻辑
            if (luseCount != -2) {
                boolean isUsing = false;
                try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}

                int maxHoldTicks = 100;
                boolean isBow = false;
                try {
                    Object inv = MappingHelper.getFieldValue(player, "inventory", null);
                    if (inv != null) {
                        int selectedSlot = ((Number) MappingHelper.getFieldValue(inv, "selectedSlot", null)).intValue();
                        Object main = MappingHelper.getFieldValue(inv, "main", null);
                        if (main instanceof java.util.List) {
                            Object stack = ((java.util.List<?>) main).get(selectedSlot);
                            if (stack != null && !(boolean) MappingHelper.invokeMethod(stack, "isEmpty")) {
                                Object item = MappingHelper.invokeMethod(stack, "getItem");
                                if (item != null) {
                                    String sid = item.toString().toLowerCase();
                                    boolean isTridentClass = false;
                                    try {
                                        Class<?> tridentClass = Class.forName("net.minecraft.class_1835");
                                        if (tridentClass.isInstance(item)) isTridentClass = true;
                                    } catch (Exception ignored) {}
                                    try {
                                        Class<?> tridentClass = Class.forName("net.minecraft.item.TridentItem");
                                        if (tridentClass.isInstance(item)) isTridentClass = true;
                                    } catch (Exception ignored) {}

                                    if (sid.contains("bow")) {
                                        isBow = true;
                                        maxHoldTicks = 35; // 弓拉满 35 tick
                                    } else if (sid.contains("trident") || isTridentClass) {
                                        isBow = true;
                                        maxHoldTicks = 25; // 三叉戟蓄力 25 tick
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (!isBow) {
                    // 消耗品 (食物、药水等): 持续保持按住右键，由原生机制完成进食，绝对不中途松开按键或调用 stopUsingItem
                    resetUseCooldown(client);
                    lusePressKey(client, "key.use");
                    luseActiveTicks++;

                    if (luseActiveTicks == 1) {
                        // 初始记录物品 stack 信息
                        LuseStackInfo info = getLuseStackInfo(player);
                        if (info != null && !info.isEmpty) {
                            lastLuseSlot = info.slot;
                            lastLuseItem = info.item;
                            lastLuseCount = info.count;
                        }
                    }

                    // 检查物品数量是否改变（已成功吃掉 1 个）
                    if (luseActiveTicks >= 10 && isLuseStackChanged(player)) {
                        // 物品数量变少或消耗完毕，说明 1 次进食已成功完成！
                        LuseStackInfo current = getLuseStackInfo(player);
                        if (current != null && !current.isEmpty) {
                            lastLuseSlot = current.slot;
                            lastLuseItem = current.item;
                            lastLuseCount = current.count;
                        } else {
                            lastLuseSlot = -1;
                            lastLuseItem = null;
                            lastLuseCount = -1;
                        }
                        luseActiveTicks = 0; // 重置计时器，准备吃下一个

                        if (luseCount > 0) {
                            luseCount--;
                        }
                        if (luseCount == 0) {
                            luseCount = -2;
                            luseReleaseKey(client, "key.use", false);
                            CommandDispatcher.addFeedback("§a已完成所有长按使用");
                        }
                    } else if (luseActiveTicks > 120 && !isUsing) {
                        // 如果长按了 6 秒且当前并没有在进食（例如饱食度满吃不下普通食物，或者手持非使用物品），自动停止防止死循环
                        luseCount = -2;
                        luseReleaseKey(client, "key.use", false);
                        CommandDispatcher.addFeedback("§e无法继续使用当前物品，已停止长按");
                    }
                } else {
                    // 弓箭 / 三叉戟: 3-stage 状态机 (蓄力 -> 松手射出 -> 缓冲)
                    switch (luseStage) {
                        case 0: // Initiation
                            resetUseCooldown(client);
                            lusePressKey(client, "key.use");
                            luseIncrementKeyCounter(client, "key.use");
                            luseStage = 1;
                            luseActiveTicks = 0;
                            break;

                        case 1: // Holding
                            resetUseCooldown(client);
                            lusePressKey(client, "key.use");
                            luseActiveTicks++;

                            if (luseActiveTicks >= maxHoldTicks) {
                                luseReleaseKey(client, "key.use", true); // 只有弓/三叉戟强行 stopUsingItem 松手射出
                                luseStage = 2;
                                luseDelayTicks = 0;

                                if (luseCount > 0) {
                                    luseCount--;
                                }
                                if (luseCount == 0) {
                                    luseCount = -2;
                                    CommandDispatcher.addFeedback("§a已完成所有长按使用");
                                }
                            }
                            break;

                        case 2: // Delay
                            luseDelayTicks++;
                            if (luseDelayTicks >= 8) {
                                if (luseCount != -2) {
                                    luseStage = 0;
                                }
                            }
                            break;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void triggerAttack(Object client, Object player) {
        try {
            Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
            if (target == null) target = MappingHelper.findUniqueFieldByType(client, MappingHelper.getClass("net.minecraft.class_239")); // HitResult

            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");

            // 1. 显式触发 attackBlock (WorldGuard 标记的核心)
            // 恢复同时调用模式，因为拆分逻辑导致标记失效
            if (target != null && MappingHelper.getClass("BlockHitResult").isInstance(target)) {
                if (im != null) {
                    try {
                        Object pos = MappingHelper.invokeMethod(target, "getBlockPos");
                        Object side = MappingHelper.invokeMethod(target, "getSide");
                        if (pos != null && side != null) {
                            MappingHelper.invokeMethod(im, "attackBlock", pos, side);
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 2. 调用原生 doAttack (触发伤害、横扫和挥手发包)
            boolean acted = false;
            try {
                MappingHelper.invokeMethod(client, "doAttack");
                acted = true;
            } catch (Exception ignored) {}

            // 3. 针对实体的补充逻辑
            if (!acted && target != null && MappingHelper.getClass("EntityHitResult").isInstance(target)) {
                if (im != null) {
                    Object entity = MappingHelper.invokeMethod(target, "getEntity");
                    if (entity != null) {
                        try { MappingHelper.setFieldValue(entity, "hurtResistantTime", 0); } catch (Exception ignored) {}
                        MappingHelper.invokeMethod(im, "attackEntity", player, entity);
                        acted = true;
                    }
                }
            }

            // 4. 强制视觉同步
            if (mainHand != null) {
                try {
                    MappingHelper.invokeMethod(player, "swingHand", mainHand);
                } catch (Exception ignored) {}
            }
            // 攻击后立即再次锁定蓄力，防止在同一 tick 内被重置
            try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private static void triggerItemUse(Object client, Object player) {
        try {
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");
            if (mainHand == null || im == null) return;

            // 1. 调用原生 doItemUse (原生处理钓鱼竿抛/收、物品使用、指向真实方块放置等)
            MappingHelper.invokeMethod(client, "doItemUse");

            // 2. 补全指向空气 (MISS) 时的空中凭空放置方块 (AirPlace)
            Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
            if (target == null) target = MappingHelper.findUniqueFieldByType(client, MappingHelper.getClass("net.minecraft.class_239"));

            boolean isMiss = true;
            if (target != null) {
                try {
                    Object type = MappingHelper.invokeMethod(target, "getType");
                    if (type != null && !type.toString().equalsIgnoreCase("MISS")) {
                        isMiss = false;
                    }
                } catch (Exception ignored) {}
            }

            if (isMiss) {
                try {
                    Class<?> bhrClass = MappingHelper.getClass("BlockHitResult");
                    Class<?> vec3Class = MappingHelper.getClass("Vec3");
                    Class<?> dirClass = MappingHelper.getClass("Direction");
                    Class<?> bpClass = MappingHelper.getClass("BlockPos");

                    double px = ((Number) MappingHelper.invokeMethod(player, "getX")).doubleValue();
                    double py = ((Number) MappingHelper.invokeMethod(player, "getEyeY")).doubleValue();
                    double pz = ((Number) MappingHelper.invokeMethod(player, "getZ")).doubleValue();

                    float pitch = ((Number) MappingHelper.invokeMethod(player, "getPitch")).floatValue();
                    float yaw = ((Number) MappingHelper.invokeMethod(player, "getYaw")).floatValue();

                    double f = Math.cos(-yaw * 0.017453292F - (float) Math.PI);
                    double f1 = Math.sin(-yaw * 0.017453292F - (float) Math.PI);
                    double f2 = -Math.cos(-pitch * 0.017453292F);
                    double f3 = Math.sin(-pitch * 0.017453292F);

                    double hitX = px + f1 * f2 * 2.0;
                    double hitY = py + f3 * 2.0;
                    double hitZ = pz + f * f2 * 2.0;

                    Object pos = vec3Class.getConstructor(double.class, double.class, double.class).newInstance(hitX, hitY, hitZ);
                    Object side = MappingHelper.getEnumConstant("Direction", "UP");
                    Object blockPos = bpClass.getConstructor(int.class, int.class, int.class)
                            .newInstance((int) Math.floor(hitX), (int) Math.floor(hitY), (int) Math.floor(hitZ));

                    if (pos != null && side != null && blockPos != null && bhrClass != null) {
                        Object airHitResult = bhrClass.getConstructor(vec3Class, dirClass, bpClass, boolean.class)
                                .newInstance(pos, side, blockPos, false);
                        MappingHelper.invokeMethod(im, "interactBlock", player, mainHand, airHitResult);
                    }
                } catch (Exception ignored) {}
            }

            // 3. 触发挥手动画
            boolean isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem");
            if (!isUsing) MappingHelper.invokeMethod(player, "swingHand", mainHand);
        } catch (Exception ignored) {}
    }


    private static void resetAttackCooldown(Object client) {
        try {
            // MinecraftClient.attackCooldown: field_1752 (1.21.1), field_1755 (1.21.4)
            String[] fields = {"field_1752", "field1752", "field_1755", "field1755", "attackCooldown"};
            for (String f : fields) {
                try { MappingHelper.setFieldValue(client, f, 0); } catch (Exception ignored) {}
            }

            // 扫描任何看起来像冷却的 int 字段
            try {
                for (java.lang.reflect.Field f : client.getClass().getDeclaredFields()) {
                    if (f.getType() == int.class && (f.getName().contains("Cooldown") || f.getName().contains("field_175"))) {
                        f.setAccessible(true);
                        f.setInt(client, 0);
                    }
                }
            } catch (Exception ignored) {}

            Object player = CommandDispatcher.getClientPlayer();
            if (player != null) {
                // 重置玩家攻击强度: field_6010 (lastAttackedTicks)
                try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}

                // 停止使用物品（如果正在使用）
                boolean isUsing = false;
                try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}
                if (isUsing) {
                    Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                    if (im != null) {
                        try { MappingHelper.invokeMethod(im, "stopUsingItem", player); } catch (Exception ignored) {}
                        try { MappingHelper.invokeMethod(im, "method_2907", player); } catch (Exception ignored) {}
                    }
                }

                // 暴力重置目标无敌时间
                Object target = null;
                String[] targetFields = {"field_1765", "field1765", "crosshairTarget"};
                for (String f : targetFields) {
                    try { target = MappingHelper.getFieldValue(client, f, null); if (target != null) break; } catch (Exception ignored) {}
                }

                if (target != null && target.getClass().getName().contains("class_3966")) { // EntityHitResult
                    Object entity = null;
                    try { entity = MappingHelper.invokeMethod(target, "method_17770"); } catch (Exception ignored) {}
                    try { if (entity == null) entity = MappingHelper.invokeMethod(target, "getEntity"); } catch (Exception ignored) {}

                    if (entity != null) {
                        try { MappingHelper.setFieldValue(entity, "field_6008", 0); } catch (Exception ignored) {} // hurtResistantTime
                        try { MappingHelper.setFieldValue(entity, "field_6007", 0); } catch (Exception ignored) {} // hurtTime
                    }
                }
            }

            // 设置 ClientPlayerInteractionManager.blockBreakingCooldown 为 0
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            if (im != null) {
                try { MappingHelper.setFieldValue(im, "field_1613", 0); } catch (Exception ignored) {}
                try { MappingHelper.setFieldValue(im, "field1613", 0); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void resetUseCooldown(Object client) {
        try {
            // itemUseCooldown: field_1753 (1.21.1), field_1752 (1.21.4)
            String[] fields = {"field_1753", "field1753", "field_1752", "field1752", "itemUseCooldown"};
            for (String f : fields) {
                try { MappingHelper.setFieldValue(client, f, 0); } catch (Exception ignored) {}
            }

            try {
                for (java.lang.reflect.Field f : client.getClass().getDeclaredFields()) {
                    if (f.getType() == int.class && (f.getName().contains("Cooldown") || f.getName().contains("field_175"))) {
                        f.setAccessible(true);
                        f.setInt(client, 0);
                    }
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }


    private static void pressKeyTranslation(Object client, String translationKey) throws Exception {
        Object kb = findKeyBinding(client, translationKey);
        if (kb != null) {
            setKeyBindingPressed(kb, true);
        }
    }

    private static void releaseKeyTranslation(Object client, String translationKey) throws Exception {
        Object kb = findKeyBinding(client, translationKey);
        if (kb != null) {
            setKeyBindingPressed(kb, false);
            resetKeyBindingCounter(kb);
        }
    }

    private static void incrementKeyCounter(Object client, String translationKey) {
        try {
            Object kb = findKeyBinding(client, translationKey);
            if (kb != null) {
                incrementKeyBindingCounter(kb);
            }
        } catch (Exception ignored) {}
    }

    private static Object findKeyBinding(Object client, String translationKey) throws Exception {
        return luseFindKeyBinding(client, translationKey);
    }

    private static void lusePressKey(Object client, String translationKey) throws Exception {
        Object kb = luseFindKeyBinding(client, translationKey);
        if (kb != null) {
            setKeyBindingPressed(kb, true);
        }
    }

    private static void luseReleaseKey(Object client, String translationKey, boolean forceStopUsing) throws Exception {
        Object kb = luseFindKeyBinding(client, translationKey);
        if (kb != null) {
            setKeyBindingPressed(kb, false);
            resetKeyBindingCounter(kb);
        }

        // 显式调用 stopUsingItem 仅在手持弓箭、三叉戟(forceStopUsing)按键释放时触发，确保绝对不打断食物/药水进食
        if (forceStopUsing) {
            Object player = CommandDispatcher.getClientPlayer();
            if (player != null) {
                boolean isUsing = false;
                try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}
                if (isUsing) {
                    Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                    if (im != null) {
                        try { MappingHelper.invokeMethod(im, "stopUsingItem", player); } catch (Exception ignored) {}
                        try { MappingHelper.invokeMethod(im, "method_2907", player); } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private static void luseReleaseKey(Object client, String translationKey) throws Exception {
        luseReleaseKey(client, translationKey, false);
    }

    private static void luseIncrementKeyCounter(Object client, String translationKey) {
        try {
            Object kb = luseFindKeyBinding(client, translationKey);
            if (kb != null) {
                incrementKeyBindingCounter(kb);
            }
        } catch (Exception ignored) {}
    }

    private static void setKeyBindingPressed(Object kb, boolean pressed) {
        if (kb == null) return;
        try { MappingHelper.setFieldValue(kb, "pressed", pressed); } catch (Exception ignored) {}
        try { MappingHelper.setFieldValue(kb, "isDown", pressed); } catch (Exception ignored) {}
        try { MappingHelper.invokeMethod(kb, "setPressed", pressed); } catch (Exception ignored) {}
        try { MappingHelper.invokeMethod(kb, "setDown", pressed); } catch (Exception ignored) {}
    }

    private static void resetKeyBindingCounter(Object kb) {
        if (kb == null) return;
        try { MappingHelper.setFieldValue(kb, "field_1661", 0); } catch (Exception ignored) {}
        try { MappingHelper.setFieldValue(kb, "field_1652", 0); } catch (Exception ignored) {}
        try { MappingHelper.setFieldValue(kb, "clickCount", 0); } catch (Exception ignored) {}
    }

    private static void incrementKeyBindingCounter(Object kb) {
        if (kb == null) return;
        for (String fName : new String[]{"field_1661", "field_1652", "clickCount"}) {
            try {
                int count = ((Number) MappingHelper.getFieldValue(kb, fName, null)).intValue();
                MappingHelper.setFieldValue(kb, fName, count + 1);
                return;
            } catch (Exception ignored) {}
        }
    }

    private static Object luseFindKeyBinding(Object client, String translationKey) throws Exception {
        Object options = MappingHelper.getFieldValue(client, "options", null);
        if (options == null) return null;
        Class<?> kbClass = MappingHelper.getClass("KeyBinding");

        Class<?> curr = options.getClass();
        while (curr != null && curr != Object.class) {
            for (java.lang.reflect.Field f : curr.getDeclaredFields()) {
                if (kbClass.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object kb = f.get(options);
                        if (kb != null) {
                            String tk = getKeyBindingTranslationKey(kb, kbClass);
                            if (translationKey.equals(tk)) {
                                return kb;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            curr = curr.getSuperclass();
        }

        try {
            java.util.Map<?, ?> allKbs = (java.util.Map<?, ?>) MappingHelper.getFieldValue(null, "keysById", kbClass);
            if (allKbs != null) {
                Object kb = allKbs.get(translationKey);
                if (kb != null) return kb;
            }
        } catch (Exception ignored) {}
        try {
            java.util.Map<?, ?> allKbs = (java.util.Map<?, ?>) MappingHelper.getFieldValue(null, "ALL", kbClass);
            if (allKbs != null) {
                Object kb = allKbs.get(translationKey);
                if (kb != null) return kb;
            }
        } catch (Exception ignored) {}
        try {
            java.util.Map<?, ?> allKbs = (java.util.Map<?, ?>) MappingHelper.getFieldValue(null, "field_1655", kbClass);
            if (allKbs != null) {
                Object kb = allKbs.get(translationKey);
                if (kb != null) return kb;
            }
        } catch (Exception ignored) {}
        try {
            java.util.Map<?, ?> allKbs = (java.util.Map<?, ?>) MappingHelper.getFieldValue(null, "field_1657", kbClass);
            if (allKbs != null) {
                Object kb = allKbs.get(translationKey);
                if (kb != null) return kb;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static String getKeyBindingTranslationKey(Object kb, Class<?> kbClass) {
        try {
            Object res = MappingHelper.invokeMethod(kb, "getName");
            if (res instanceof String && ((String) res).startsWith("key.")) return (String) res;
        } catch (Exception ignored) {}
        try {
            Object res = MappingHelper.invokeMethod(kb, "getCategory");
            if (res instanceof String && ((String) res).startsWith("key.")) return (String) res;
        } catch (Exception ignored) {}

        String[] candidateFields = {"name", "translationKey", "field_1654", "field_1660"};
        for (String fName : candidateFields) {
            try {
                String val = (String) MappingHelper.getFieldValue(kb, fName, kbClass);
                if (val != null && val.startsWith("key.")) return val;
            } catch (Exception ignored) {}
        }

        for (java.lang.reflect.Field kf : kb.getClass().getDeclaredFields()) {
            if (kf.getType() == String.class) {
                try {
                    kf.setAccessible(true);
                    String val = (String) kf.get(kb);
                    if (val != null && val.startsWith("key.")) return val;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
