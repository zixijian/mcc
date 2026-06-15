package net.mcc;

public class AutomationManager {
    private static int attackFreq = -1;
    private static int useFreq = -1;
    private static int attackTimer = 0;
    private static int useTimer = 0;
    private static boolean attackOnce = false;
    private static boolean useOnce = false;
    private static boolean eatingMode = false;
    private static Object eatingItem = null;
    private static int eatingInitialCount = -1;
    private static boolean autoRespawn = false;


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
            try {
                Object player = CommandDispatcher.getClientPlayer();
                if (player != null) {
                    Object stack = MappingHelper.invokeMethod(player, "getMainHandStack");
                    if (stack != null && !((boolean) MappingHelper.invokeMethod(stack, "isEmpty"))) {
                        // 智能识别：使用 UseAction 判断是否需要进入持续模式 (食物/药水)
                        Object action = MappingHelper.invokeMethod(stack, "getUseAction");
                        String actionName = (action != null) ? action.toString() : "";

                        Object item = MappingHelper.invokeMethod(stack, "getItem");
                        String registryName = "";
                        try {
                            Object id = MappingHelper.invokeMethod(MappingHelper.getRegistry("ITEM"), "getId", item);
                            registryName = id.toString();
                        } catch (Exception ignored) {}

                        // EAT / DRINK 且不是喷溅药水
                        boolean isConsumable = actionName.contains("EAT") || actionName.contains("DRINK");
                        if (isConsumable && (registryName.contains("splash") || registryName.contains("lingering"))) {
                            isConsumable = false;
                        }

                        if (isConsumable) {
                            eatingMode = true;
                            eatingItem = stack;
                            try {
                                eatingInitialCount = ((Number) MappingHelper.invokeMethod(stack, "getCount")).intValue();
                            } catch (Exception e) { eatingInitialCount = -1; }
                            useFreq = -1;
                            attackFreq = -1;

                            // 立即同步触发，消除首次启动延迟
                            Object client = CommandDispatcher.getClient();
                            resetUseCooldown(client);
                            pressKeyTranslation(client, "key.use");
                            triggerItemUse(client, player);

                            CommandDispatcher.addFeedback("§a开始消耗物品...");
                            return;
                        }
                    }

                    // 单次即时使用：火箭、鱼竿、工具
                    Object client = CommandDispatcher.getClient();
                    resetUseCooldown(client);
                    triggerItemUse(client, player);
                    // 注意：单次使用直接同步完成，不设置 useOnce 标志位（防止双重触发）
                    CommandDispatcher.addFeedback("§a动作已执行");
                    return;
                }
            } catch (Exception e) {
                CommandDispatcher.addFeedback("§c执行失败: " + e.getMessage());
            }
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
        attackOnce = useOnce = eatingMode = false;
        eatingItem = null;
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

            // 健壮的当前屏幕检测
            Object currentScreen = null;
            try {
                currentScreen = MappingHelper.getFieldValue(client, "currentScreen", null);
            } catch (Exception e) {
                try {
                    Class<?> screenClass = MappingHelper.getClass("Screen");
                    currentScreen = MappingHelper.findUniqueFieldByType(client, screenClass);
                } catch (Exception ignored) {}
            }
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
            if (eatingMode) {
                Object currentStack = MappingHelper.invokeMethod(player, "getMainHandStack");
                boolean isUsing = false;
                try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}

                boolean finished = false;
                if (currentStack == null) {
                    finished = true;
                } else {
                    try {
                        if ((boolean) MappingHelper.invokeMethod(currentStack, "isEmpty")) {
                            finished = true;
                        } else {
                            int currentCount = ((Number) MappingHelper.invokeMethod(currentStack, "getCount")).intValue();
                            if (currentCount < eatingInitialCount) {
                                finished = true; // 数量减少了，说明吃掉了一个
                            } else {
                                // 检查物品类型是否改变（针对药水变瓶子，或者某些特殊物品）
                                Object oldItem = MappingHelper.invokeMethod(eatingItem, "getItem");
                                Object newItem = MappingHelper.invokeMethod(currentStack, "getItem");
                                if (oldItem != newItem) {
                                    finished = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (!isUsing) finished = true;
                    }
                }

                if (finished) {
                    eatingMode = false;
                    eatingItem = null;
                    eatingInitialCount = -1;
                    releaseKeyTranslation(client, "key.use");
                    CommandDispatcher.addFeedback("§a动作完成");
                    return;
                }

                // 进食模式：保持按键按下并尝试触发
                pressKeyTranslation(client, "key.use");
                if (!isUsing) triggerItemUse(client, player);
            } else if (useOnce) {
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
        } catch (Throwable ignored) {}
    }

    private static void triggerAttack(Object client, Object player) {
        try {
            Object target = null;
            String[] targetFields = {"field_1765", "field1765", "crosshairTarget"};
            for (String f : targetFields) {
                try { target = MappingHelper.getFieldValue(client, f, null); if (target != null) break; } catch (Exception ignored) {}
            }

            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            boolean attacked = false;

            if (target != null && target.getClass().getName().contains("class_3966")) { // EntityHitResult
                Object entity = null;
                try { entity = MappingHelper.invokeMethod(target, "method_17770"); } catch (Exception ignored) {}
                try { if (entity == null) entity = MappingHelper.invokeMethod(target, "getEntity"); } catch (Exception ignored) {}

                if (entity != null && im != null) {
                    // 移除伤害无敌帧
                    try { MappingHelper.setFieldValue(entity, "field_6008", 0); } catch (Exception ignored) {} // hurtResistantTime
                    try { MappingHelper.setFieldValue(entity, "field_6007", 0); } catch (Exception ignored) {} // hurtTime

                    // 直接调用 interactionManager.attackEntity
                    try {
                        MappingHelper.invokeMethod(im, "attackEntity", player, entity);
                        attacked = true;
                    } catch (Exception ignored) {}
                }
            }

            if (!attacked) {
                // 执行常规攻击 (doAttack)
                String[] methods = {"method_1536", "method1536", "doAttack"};
                for (String m : methods) {
                    try { MappingHelper.invokeMethod(client, m); break; } catch (Exception ignored) {}
                }
            }

            // 显式触发一次挥手
            try {
                Class<?> handClass = MappingHelper.getClass("Hand");
                Object mainHand = MappingHelper.getFieldValue(null, "MAIN_HAND", handClass);
                String[] swingMethods = {"method_6104", "method6104", "swingHand"};
                for (String m : swingMethods) {
                    try { MappingHelper.invokeMethod(player, m, mainHand); break; } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private static void triggerItemUse(Object client, Object player) {
        try {
            // 强制重置冷却，确保立即触发
            resetUseCooldown(client);

            Class<?> handClass = MappingHelper.getClass("Hand");
            Object mainHand = MappingHelper.getFieldValue(null, "MAIN_HAND", handClass);
            if (mainHand == null) return;

            Object stack = MappingHelper.invokeMethod(player, "getMainHandStack");
            Object item = (stack != null) ? MappingHelper.invokeMethod(stack, "getItem") : null;
            String registryName = "";
            if (item != null) {
                try {
                    Object id = MappingHelper.invokeMethod(MappingHelper.getRegistry("ITEM"), "getId", item);
                    registryName = id.toString();
                } catch (Exception ignored) {}
            }

            // 1. 鱼竿特殊处理：强制直接调用交互方法，绕过 doItemUse 的视线检查逻辑
            if (registryName.contains("fishing_rod")) {
                Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                if (im != null) {
                    try {
                        MappingHelper.invokeMethod(im, "interactItem", player, mainHand);
                        MappingHelper.invokeMethod(player, "swingHand", mainHand);
                        return;
                    } catch (Exception ignored) {}
                }
            }

            // 2. 核心逻辑：调用标准的 doItemUse()
            // 这一步会处理火箭、工具交互、方块点击以及进食启动
            boolean done = false;
            String[] methods = {"method_1531", "method1531", "doItemUse"};
            for (String m : methods) {
                try {
                    MappingHelper.invokeMethod(client, m);
                    done = true;
                    break;
                } catch (Exception ignored) {}
            }

            // 3. 兜底策略：如果 doItemUse 失败，尝试直接 interactItem
            if (!done) {
                Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                if (im != null) {
                    try { MappingHelper.invokeMethod(im, "interactItem", player, mainHand); } catch (Exception ignored) {}
                }
            }

            // 4. 同步补齐视觉效果
            try {
                MappingHelper.invokeMethod(player, "swingHand", mainHand);
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
