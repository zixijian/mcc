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

    public static void setLuse(int count) {
        luseCount = count;
        luseStage = 0;
        luseDelayTicks = 0;
        luseActiveTicks = 0;
        luseStarted = false;
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

    /**
     * 客户端 Tick 回调
     */
    public static void onClientTick() {
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
                incrementKeyCounter(client, "key.use");
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
                    incrementKeyCounter(client, "key.use");
                    triggerItemUse(client, player);
                    useTimer = useFreq;
                }
            }

            // 4. Luse (长按使用) 逻辑
            if (luseCount != -2) {
                // 如果当前屏幕不是 null，用户指令是不中断该状态（只有 stop 停止），
                // 但如果打开了 GUI，我们不能让它由于在 GUI 中乱发包或者按键锁定而导致问题。
                // 按照 memory 中的模式，我们可以：
                // 如果当前有 GUI，为了安全可能需要暂时在 Tick 中不执行状态机动作，但保留其状态，
                // 或者说，如果 screen 存在，我们不更新状态机。不过上面的代码一开头就有：
                // if (currentScreen != null) return;
                // 这意味着如果 currentScreen != null，整个 onClientTick 早就 return 了。
                // 这说明只要在 GUI 中，onClientTick 就不会走。这也符合“状态保留，只有 stop 能彻底停止”的要求，
                // 因为一旦关闭 GUI 回到游戏，onClientTick 会继续，状态机可以继续跑！

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
                                        maxHoldTicks = 35; // 弓拉满改到 35 tick
                                    } else if (sid.contains("trident") || isTridentClass) {
                                        isBow = true;
                                        maxHoldTicks = 25; // 三叉戟蓄力改到 25 tick
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                switch (luseStage) {
                    case 0: // Stage 0: Initiation (启动/触发阶段)
                        resetUseCooldown(client);
                        lusePressKey(client, "key.use");
                        luseIncrementKeyCounter(client, "key.use");

                        // 为了避免双重手swing或打断动画，只在第一 tick 尝试一次 interactItem
                        Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                        Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");
                        if (im != null && mainHand != null) {
                            try {
                                MappingHelper.invokeMethod(im, "interactItem", player, mainHand);
                            } catch (Exception ignored) {}
                        }

                        luseStage = 1;
                        luseActiveTicks = 0;
                        luseStarted = false;
                        break;

                    case 1: // Stage 1: Holding (持续按住阶段)
                        resetUseCooldown(client); // 持续重置右键冷却，确保第二次拉弓能立即启动不被 4 ticks 延迟导致少蓄力
                        lusePressKey(client, "key.use");
                        luseActiveTicks++;

                        if (isUsing) {
                            luseStarted = true;
                        }

                        // 判定单次使用动作完成或中断的条件：
                        // 如果开始使用过（luseStarted = true）且当前不再使用（!isUsing），或者长按超过了一定安全时长（如 100 ticks）
                        if ((!isBow && luseStarted && !isUsing) || (isBow && luseActiveTicks >= maxHoldTicks) || luseActiveTicks > 100) {
                            luseReleaseKey(client, "key.use");
                            luseStage = 2;
                            luseDelayTicks = 0;

                            if (luseCount > 0) {
                                luseCount--;
                            }
                            if (luseCount == 0) {
                                // 所有次数执行完毕，停止
                                luseCount = -2;
                                CommandDispatcher.addFeedback("§a已完成所有长按使用");
                            }
                        }
                        break;

                    case 2: // Stage 2: Delay (延迟/缓冲阶段)
                        luseDelayTicks++;
                        if (luseDelayTicks >= 8) {
                            if (luseCount != -2) {
                                luseStage = 0;
                            }
                        }
                        break;
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

            // 检查当前手持物品是否为钓鱼竿
            boolean isFishingRod = false;
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
                                // 1. 优先采用 Class 类型进行匹配，完全免疫混淆和无界面打包等复杂环境
                                try {
                                    Class<?> rodClass = MappingHelper.getClass("FishingRodItem");
                                    if (rodClass.isInstance(item)) {
                                        isFishingRod = true;
                                    }
                                } catch (Exception ignored) {}

                                // 2. 兜底策略：字符串及注册表查询
                                if (!isFishingRod) {
                                    String sid = item.toString();
                                    if (sid.contains("fishing_rod")) {
                                        isFishingRod = true;
                                    } else {
                                        Object registry = MappingHelper.getRegistry("ITEM");
                                        if (registry != null) {
                                            Object identifier = MappingHelper.invokeMethod(registry, "getId", item);
                                            if (identifier != null && identifier.toString().contains("fishing_rod")) {
                                                isFishingRod = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (isFishingRod) {
                // 针对钓鱼竿，仅调用 interactItem 并跳过 doItemUse，配合 useOnce 等逻辑防止同一 tick 内双重交互导致“cast-then-reel-in”仅见挥手
                MappingHelper.invokeMethod(im, "interactItem", player, mainHand);
            } else {
                // 1. 原生 doItemUse (处理放置、火箭、拉弓等)
                MappingHelper.invokeMethod(client, "doItemUse");

                // 2. 深度补充 interactBlock (针对 experimental 1.21.11 的木axe等特定插件)
                Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
                if (target == null) target = MappingHelper.findUniqueFieldByType(client, MappingHelper.getClass("net.minecraft.class_239"));
                if (target == null) {
                    for (java.lang.reflect.Field f : client.getClass().getDeclaredFields()) {
                        if (f.getType().getName().contains("class_239") || f.getType().getSimpleName().contains("HitResult")) {
                            f.setAccessible(true); target = f.get(client); if (target != null) break;
                        }
                    }
                }

                if (target != null && MappingHelper.getClass("BlockHitResult").isInstance(target)) {
                    Object res = MappingHelper.invokeMethod(im, "interactBlock", player, mainHand, target);
                    if (res != null) {
                        boolean accepted = false;
                        try { accepted = (boolean) MappingHelper.invokeMethod(res, "isAccepted"); } catch (Exception e) {
                            if (String.valueOf(res).contains("SUCCESS") || String.valueOf(res).contains("CONSUME")) accepted = true;
                        }
                        if (accepted) MappingHelper.invokeMethod(client, "doItemUse"); // 同步客户端状态
                    }
                }

                // 3. 深度补充 interactItem (钓鱼竿、喷溅药水、末影珍珠)
                MappingHelper.invokeMethod(im, "interactItem", player, mainHand);
            }

            // 4. 强制触发挥手
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
            MappingHelper.setFieldValue(kb, "pressed", true);
            try { MappingHelper.invokeMethod(kb, "setPressed", true); } catch (Exception ignored) {}
        }
    }

    private static void releaseKeyTranslation(Object client, String translationKey) throws Exception {
        Object kb = findKeyBinding(client, translationKey);
        if (kb != null) {
            MappingHelper.setFieldValue(kb, "pressed", false);
            try { MappingHelper.setFieldValue(kb, "field_1652", 0); } catch (Exception ignored) {}
            try { MappingHelper.invokeMethod(kb, "setPressed", false); } catch (Exception ignored) {}
        }
    }

    private static void incrementKeyCounter(Object client, String translationKey) {
        try {
            Object kb = findKeyBinding(client, translationKey);
            if (kb != null) {
                int count = ((Number) MappingHelper.getFieldValue(kb, "field_1652", null)).intValue();
                MappingHelper.setFieldValue(kb, "field_1652", count + 1);
            }
        } catch (Exception ignored) {}
    }

    private static Object findKeyBinding(Object client, String translationKey) throws Exception {
        Object options = MappingHelper.getFieldValue(client, "options", null);
        Class<?> kbClass = MappingHelper.getClass("KeyBinding");
        Class<?> curr = options.getClass();
        while (curr != null && curr != Object.class) {
            for (java.lang.reflect.Field f : curr.getDeclaredFields()) {
                if (kbClass.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object kb = f.get(options);
                        if (kb != null) {
                            String tk = (String) MappingHelper.getFieldValue(kb, "translationKey", kbClass);
                            if (translationKey.equals(tk)) return kb;
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
        return null;
    }

    private static void lusePressKey(Object client, String translationKey) throws Exception {
        Object kb = luseFindKeyBinding(client, translationKey);
        if (kb != null) {
            MappingHelper.setFieldValue(kb, "pressed", true);
            try { MappingHelper.invokeMethod(kb, "setPressed", true); } catch (Exception ignored) {}
        }
    }

    private static void luseReleaseKey(Object client, String translationKey) throws Exception {
        Object kb = luseFindKeyBinding(client, translationKey);
        if (kb != null) {
            MappingHelper.setFieldValue(kb, "pressed", false);
            try { MappingHelper.setFieldValue(kb, "field_1661", 0); } catch (Exception ignored) {}
            try { MappingHelper.invokeMethod(kb, "setPressed", false); } catch (Exception ignored) {}
        }

        // 显式调用 stopUsingItem 确保弓箭、三叉戟在按键释放时绝对、即时触发释放攻击/抛出
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

    private static void luseIncrementKeyCounter(Object client, String translationKey) {
        try {
            Object kb = luseFindKeyBinding(client, translationKey);
            if (kb != null) {
                int count = ((Number) MappingHelper.getFieldValue(kb, "field_1661", null)).intValue();
                MappingHelper.setFieldValue(kb, "field_1661", count + 1);
            }
        } catch (Exception ignored) {}
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
                            String tk = null;
                            try {
                                tk = (String) MappingHelper.getFieldValue(kb, "field_1654", kbClass);
                            } catch (Exception ignored) {}
                            if (tk == null) {
                                try {
                                    tk = (String) MappingHelper.getFieldValue(kb, "field_1660", kbClass);
                                } catch (Exception ignored) {}
                            }
                            if (tk == null) {
                                for (java.lang.reflect.Field kf : kb.getClass().getDeclaredFields()) {
                                    if (kf.getType() == String.class) {
                                        kf.setAccessible(true);
                                        String val = (String) kf.get(kb);
                                        if (val != null && val.startsWith("key.")) {
                                            tk = val;
                                            break;
                                        }
                                    }
                                }
                            }
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
}
