package net.mcc;

public class AutomationManager {
    private static int attackFreq = -1;
    private static int useFreq = -1;
    private static int attackTimer = 0;
    private static int useTimer = 0;
    private static boolean attackOnce = false;
    private static boolean useOnce = false;
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
                if (target != null && target.getClass().getName().contains("class_3965") && im != null) { // BlockHitResult
                    try {
                        Object pos = MappingHelper.invokeMethod(target, "getBlockPos");
                        Object side = MappingHelper.invokeMethod(target, "getSide");
                        if (pos != null && side != null) {
                            MappingHelper.invokeMethod(im, "attackBlock", pos, side);
                            attacked = true;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (!attacked) {
                // 执行常规攻击 (doAttack)
                String[] methods = {"method_1536", "method1536", "method_1587", "method1587", "doAttack"};
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
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Class<?> handClass = MappingHelper.getClass("Hand");
            Object mainHand = MappingHelper.getFieldValue(null, "MAIN_HAND", handClass);

            if (mainHand == null || im == null) return;

            // 1. 尝试常规 doItemUse (MinecraftClient)
            // 它会处理大多数逻辑（方块交互、使用物品）
            String[] methods = {"method_1531", "method1531", "method_1583", "method1583", "doItemUse"};
            for (String m : methods) {
                try { MappingHelper.invokeMethod(client, m); break; } catch (Exception ignored) {}
            }

            // 2. 补充调用 interactItem (确保钓鱼竿、药水在 doItemUse 未能成功触发时执行)
            // 针对 1.21.4+: method_2919 是 interactItem
            // 针对 1.21.1: method_2896 是 interactItem
            String[] interactMethods = {"method_2896", "method2896", "method_2919", "method2919", "interactItem"};
            for (String m : interactMethods) {
                try { MappingHelper.invokeMethod(im, m, player, mainHand); break; } catch (Exception ignored) {}
            }

            // 3. 显式触发挥手 (提供视觉反馈)
            try {
                boolean isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem");
                if (!isUsing) {
                    String[] swingMethods = {"method_6104", "method6104", "swingHand"};
                    for (String m : swingMethods) {
                        try { MappingHelper.invokeMethod(player, m, mainHand); break; } catch (Exception ignored) {}
                    }
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
