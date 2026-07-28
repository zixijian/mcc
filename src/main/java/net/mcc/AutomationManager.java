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

    private static int internalAttackTicks = 0;
    private static int waitTicksAfterHalfCharge = -1;

    private static Float lockedPitch = null;
    private static Float lockedYaw = null;

    private static int longPressUseCount = -1;
    private static int longPressUseStage = 0; // 0 = 未开始, 1 = 已启动/等待中, 2 = 保持中, 3 = 延迟中
    private static int longPressUseDelayCount = 0;
    private static int longPressTimeout = 0;

    public static void setLongPressUse(int count, boolean hasArgs) {
        if (!hasArgs) {
            longPressUseCount = 1;
            longPressUseStage = 0;
            longPressUseDelayCount = 0;
            longPressTimeout = 0;
            CommandDispatcher.addFeedback("§a模拟长按使用一次");
            return;
        }
        if (count < 0) {
            longPressUseCount = -1;
            longPressUseStage = 0;
            longPressUseDelayCount = 0;
            longPressTimeout = 0;
            try {
                Object client = CommandDispatcher.getClient();
                releaseKeyTranslation(client, "key.use");
            } catch (Exception ignored) {}
            CommandDispatcher.addFeedback("§a关闭长按使用");
            return;
        }
        longPressUseCount = count;
        longPressUseStage = 0;
        longPressUseDelayCount = 0;
        longPressTimeout = 0;
        CommandDispatcher.addFeedback("§a长按使用任务已设定: " + (count == 0 ? "无限循环" : count + " 次"));
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

    public static void toggleAutoRespawn() {
        autoRespawn = !autoRespawn;
        CommandDispatcher.addFeedback("§a自动复活: " + (autoRespawn ? "开启" : "关闭"));
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
        longPressUseCount = -1;
        longPressUseStage = 0;
        longPressUseDelayCount = 0;
        longPressTimeout = 0;
        try {
            Object client = CommandDispatcher.getClient();
            releaseKeyTranslation(client, "key.attack");
            releaseKeyTranslation(client, "key.use");
        } catch (Exception e) {}
        CommandDispatcher.addFeedback("§e已停止所有自动行为");
    }

    public static void showStatus() {
        CommandDispatcher.addFeedback(String.format("§b[MCC] Atk:%d Use:%d Luse:%d Rsp:%b", attackFreq, useFreq, longPressUseCount, autoRespawn));
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

            // 1. 自动复活
            if (autoRespawn) {
                float hp = ((Number) MappingHelper.invokeMethod(player, "getHealth")).floatValue();
                if (hp <= 0) MappingHelper.invokeMethod(player, "requestRespawn");
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

            // 4. 长按使用逻辑 (luse)
            if (longPressUseCount >= 0) {
                resetUseCooldown(client);

                boolean isUsing = false;
                try {
                    isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem");
                } catch (Exception ignored) {}

                if (longPressUseStage == 0) {
                    // 首次启动：按下 key.use 并增加计数器，同步触发交互让 Minecraft 启动使用
                    pressKeyTranslation(client, "key.use");
                    incrementKeyCounter(client, "key.use");
                    triggerItemUse(client, player);
                    longPressUseStage = 1;
                    longPressTimeout = 0;
                } else if (longPressUseStage == 1) {
                    // 已启动/等待中阶段：等待 isUsingItem 在 client 变为 true
                    pressKeyTranslation(client, "key.use");
                    if (isUsing) {
                        longPressUseStage = 2; // 正式进入进行中/保持中阶段
                    } else {
                        longPressTimeout++;
                        if (longPressTimeout > 15) { // 15 tick 超时保护（如空手或物品无法使用）
                            releaseKeyTranslation(client, "key.use");
                            longPressUseCount = -1;
                            longPressUseStage = 0;
                            CommandDispatcher.addFeedback("§c[luse] 长按超时，目标可能无法使用");
                        }
                    }
                } else if (longPressUseStage == 2) {
                    // 保持中阶段：只管保持按键状态，直到 isUsingItem 变为 false（完成进食/饮用）
                    pressKeyTranslation(client, "key.use");

                    if (!isUsing) {
                        // 进食完毕，释放按键
                        releaseKeyTranslation(client, "key.use");

                        boolean finished = false;
                        if (longPressUseCount > 0) {
                            longPressUseCount--;
                            if (longPressUseCount == 0) {
                                longPressUseCount = -1;
                                longPressUseStage = 0;
                                finished = true;
                                CommandDispatcher.addFeedback("§a[luse] 长按使用任务完成");
                            }
                        }

                        if (!finished) {
                            longPressUseStage = 3;
                            longPressUseDelayCount = 0;
                        }
                    }
                } else if (longPressUseStage == 3) {
                    // 延迟冷却阶段（8 ticks）
                    longPressUseDelayCount++;
                    if (longPressUseDelayCount >= 8) {
                        longPressUseStage = 0; // 冷却结束，回到未开始阶段
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
            if (!isUsing && longPressUseCount < 0) MappingHelper.invokeMethod(player, "swingHand", mainHand);
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
}
