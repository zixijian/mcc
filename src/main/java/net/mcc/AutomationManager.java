package net.mcc;

public class AutomationManager {
    private static int attackFreq = -1;
    private static int useFreq = -1;
    private static int attackTimer = 0;
    private static int useTimer = 0;
    private static boolean attackOnce = false;
    private static boolean useOnce = false;
    private static boolean autoRespawn = false;

    private static int smartUseTimer = 0;
    private static int lastItemCount = -1;
    private static Object lastItemType = null;

    private static Float lockedPitch = null;
    private static Float lockedYaw = null;

    public static void setAttack(int freq, boolean hasArgs) {
        if (!hasArgs) {
            attackOnce = true;
            attackTimer = 0; // 重置计时器
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
        try {
            Object client = CommandDispatcher.getClient();
            releaseKeyTranslation(client, "key.attack");
            releaseKeyTranslation(client, "key.use");
        } catch (Exception e) {}
        CommandDispatcher.addFeedback("§e已停止所有自动行为");
    }

    public static void showStatus() {
        CommandDispatcher.addFeedback(String.format("§b[MCC] Atk:%d Use:%d Rsp:%b", attackFreq, useFreq, autoRespawn));
    }

    private static int getItemCount(Object stack) {
        try {
            if (stack == null || (boolean)MappingHelper.invokeMethod(stack, "isEmpty")) return 0;
            return ((Number)MappingHelper.invokeMethod(stack, "getCount")).intValue();
        } catch (Exception e) { return 0; }
    }

    private static Object getItemType(Object stack) {
        try {
            if (stack == null) return null;
            return MappingHelper.invokeMethod(stack, "getItem");
        } catch (Exception e) { return null; }
    }

    public static void probeMappings() {
        CommandDispatcher.addFeedback("§d[MCC Mapping Probe]");
        try {
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
                String[] imMethods = {"method_2896", "method_2919", "method_2902", "method_2905", "method_2910", "interactItem", "interactBlock", "attackBlock"};
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

            // 健壮的当前屏幕检测：仅允许在无界面或聊天界面执行
            try {
                Object currentScreen = MappingHelper.getFieldValue(client, "currentScreen", null);
                if (currentScreen != null && !MappingHelper.isInstance(currentScreen, "ChatScreen")) {
                    return;
                }
            } catch (Exception ignored) {}

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
            if (attackFreq >= 0 || attackOnce) {
                // 固定永久蓄满力: field_6010 (lastAttackedTicks)
                try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}
            }

            if (attackOnce) {
                resetAttackCooldown(client);
                triggerAttack(client, player);
                attackOnce = false;
            } else if (attackFreq == 0) {
                resetAttackCooldown(client);
                triggerAttack(client, player);
            } else if (attackFreq > 0) {
                if (--attackTimer <= 0) {
                    resetAttackCooldown(client);
                    triggerAttack(client, player);
                    attackTimer = attackFreq;
                }
            } else {
                attackTimer = 0;
            }

            // 3. 使用逻辑
            Object stack = null;
            try {
                Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");
                stack = MappingHelper.invokeMethod(player, "getStackInHand", mainHand);
            } catch (Exception ignored) {}

            boolean isEatable = false;
            if (stack != null) {
                try {
                    if (!((Boolean)MappingHelper.invokeMethod(stack, "isEmpty"))) {
                        Object action = MappingHelper.invokeMethod(stack, "getUseAction");
                        if (action != null) {
                            // 使用 name() 方法获取枚举名称，比 toString() 更可靠
                            String actionName = (String) MappingHelper.invokeMethod(action, "name");
                            isEatable = "EAT".equals(actionName) || "DRINK".equals(actionName);
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (useOnce) {
                if (isEatable) {
                    smartUseTimer = 100; // 最多持续 5 秒
                    lastItemCount = getItemCount(stack);
                    lastItemType = getItemType(stack);
                    useOnce = false;
                } else {
                    resetUseCooldown(client);
                    incrementKeyCounter(client, "key.use");
                    triggerItemUse(client, player);
                    useOnce = false;
                }
            }

            if (smartUseTimer > 0) {
                if (isEatable && getItemType(stack) == lastItemType && getItemCount(stack) == lastItemCount) {
                    pressKeyTranslation(client, "key.use");
                    smartUseTimer--;
                    // 如果尚未进入“使用”状态（比如刚开始吃），显式尝试触发
                    boolean isUsing = false;
                    try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}
                    if (!isUsing) triggerItemUse(client, player);
                } else {
                    smartUseTimer = 0;
                    // 进食结束，仅在非 0 频率模式下释放按键。
                    // 0 频率模式（持续使用模式）应由主逻辑继续维持按键状态。
                    if (useFreq != 0) {
                        releaseKeyTranslation(client, "key.use");
                    }
                }
            } else if (useFreq == 0) {
                resetUseCooldown(client);

                boolean hasFishHook = false;
                try {
                    Object hook = MappingHelper.getFieldValue(player, "fishHook", null);
                    if (hook != null) hasFishHook = true;
                } catch (Exception ignored) {}

                if (hasFishHook) {
                    // 如果已经有钩子，必须释放按键，否则会自动收竿
                    releaseKeyTranslation(client, "key.use");
                } else {
                    // 恢复原本逻辑：持续按住按键以支持方块、火箭、弓箭、盾牌等
                    pressKeyTranslation(client, "key.use");

                    boolean isUsing = false;
                    try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}
                    // 如果尚未进入“使用”状态，显式触发一次交互
                    if (!isUsing) {
                        triggerItemUse(client, player);
                    }
                }
            } else if (useFreq > 0) {
                if (isEatable) {
                    if (--useTimer <= 0) {
                        smartUseTimer = 100;
                        lastItemCount = getItemCount(stack);
                        lastItemType = getItemType(stack);
                        useTimer = useFreq;
                        triggerItemUse(client, player); // 立即启动
                    }
                } else {
                    releaseKeyTranslation(client, "key.use");
                    if (--useTimer <= 0) {
                        resetUseCooldown(client);
                        incrementKeyCounter(client, "key.use");
                        triggerItemUse(client, player);
                        useTimer = useFreq;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void triggerAttack(Object client, Object player) {
        try {
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object target = null;
            try { target = MappingHelper.getFieldValue(client, "crosshairTarget", null); } catch (Exception ignored) {}
            Object hand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");

            // 1. 尝试显式方块攻击补丁 (针对 1.21.11 木斧领地标记)
            if (target != null && im != null && MappingHelper.isInstance(target, "BlockHitResult")) {
                try {
                    Object pos = MappingHelper.invokeMethod(target, "getBlockPos");
                    Object side = MappingHelper.invokeMethod(target, "getSide");
                    if (pos != null && side != null) {
                        MappingHelper.invokeMethod(im, "attackBlock", pos, side);
                    }
                } catch (Exception ignored) {}
            }

            // 2. 核心原生 doAttack (处理实体攻击、连击逻辑及 1.21.x 常规交互)
            try { MappingHelper.invokeMethod(client, "doAttack"); } catch (Exception ignored) {}

            // 3. 确保挥手
            if (hand != null) {
                try { MappingHelper.invokeMethod(player, "swingHand", hand); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void triggerItemUse(Object client, Object player) {
        try {
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object hand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");
            Object target = null;
            try { target = MappingHelper.getFieldValue(client, "crosshairTarget", null); } catch (Exception ignored) {}

            if (hand == null) return;

            // 严禁在“使用中”重复触发，否则进食/拉弓进度会瞬间归零
            try { if ((boolean) MappingHelper.invokeMethod(player, "isUsingItem")) return; } catch (Exception ignored) {}

            // 采用“原生主导 + 智能接口补偿”策略

            // A. 方块放置补丁 (仅在指向方块时优先尝试)
            if (target != null && MappingHelper.isInstance(target, "BlockHitResult") && im != null) {
                try {
                    Object res = MappingHelper.invokeMethod(im, "interactBlock", player, hand, target);
                    if (res != null && !res.toString().contains("PASS")) return;
                } catch (Exception ignored) {}
            }

            // B. 原生入口 (处理火箭、喷溅药水、及多数常规物品)
            try { MappingHelper.invokeMethod(client, "doItemUse"); } catch (Exception ignored) {}

            // C. 进食/药水补偿补丁 (针对某些版本原生入口无法触发饮食的情况)
            try {
                if (!(boolean) MappingHelper.invokeMethod(player, "isUsingItem") && im != null) {
                    MappingHelper.invokeMethod(im, "interactItem", player, hand);
                }
            } catch (Exception ignored) {}

            // D. 确保视觉反馈
            try {
                if (!(boolean) MappingHelper.invokeMethod(player, "isUsingItem")) {
                    MappingHelper.invokeMethod(player, "swingHand", hand);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }


    private static void resetAttackCooldown(Object client) {
        try {
            // MinecraftClient.attackCooldown: field_1752 (1.21.1), field_1755 (1.21.4)
            String[] fields = {"field_1752", "field1752", "field_1755", "field1755", "attackCooldown"};
            for (String f : fields) {
                try { MappingHelper.setFieldValue(client, f, 0); } catch (Exception ignored) {}
            }

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
