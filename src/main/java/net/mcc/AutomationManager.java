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


    private static Float lockedPitch = null;
    private static Float lockedYaw = null;

    public static void setAttack(int freq, boolean hasArgs) {
        if (!hasArgs) {
            // 立即调度执行一次
            try {
                Object client = CommandDispatcher.getClient();
                Object player = CommandDispatcher.getClientPlayer();
                if (client != null && player != null) {
                    // 尝试在客户端主线程执行，确保立即生效且不崩溃
                    try {
                        MappingHelper.invokeMethod(client, "execute", (Runnable) () -> triggerAttack(client, player));
                    } catch (Throwable e) {
                        triggerAttack(client, player);
                    }
                }
            } catch (Throwable ignored) {}
            attackOnce = true;
            attackTimer = 0;
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

            // 健壮的当前屏幕检测
            Object currentScreen = null;
            try {
                Class<?> screenClass = MappingHelper.getClass("Screen");
                currentScreen = MappingHelper.findUniqueFieldByType(client, screenClass);
            } catch (Exception ignored) {}

            // 允许在聊天界面（ChatScreen）打开时继续执行自动化任务，方便从聊天框调试
            if (currentScreen != null) {
                String screenName = currentScreen.getClass().getName();
                if (!screenName.contains("class_408") && !screenName.contains("ChatScreen")) {
                    return;
                }
            }

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
        if (client == null || player == null) return;
        try {
            resetAttackCooldown(client);

            Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
            if (target == null) target = MappingHelper.findUniqueFieldByType(client, MappingHelper.getClass("net.minecraft.class_239"));

            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");

            // 1. 环境补正
            try {
                MappingHelper.invokeMethod(player, "setSprinting", false);
                MappingHelper.setFieldValue(player, "field_6012", true);
                MappingHelper.setFieldValue(player, "field_6010", 100);
            } catch (Throwable ignored) {}

            // 2. 目标识别
            boolean isArmorStand = false;
            Object targetEntity = null;
            boolean isEntity = false;
            try {
                if (target != null && MappingHelper.getClass("EntityHitResult").isInstance(target)) {
                    isEntity = true;
                    targetEntity = MappingHelper.invokeMethod(target, "getEntity");
                    if (targetEntity != null && MappingHelper.getClass("ArmorStand").isInstance(targetEntity)) {
                        isArmorStand = true;
                    }
                }
            } catch (Throwable ignored) {}

            // 3. 动作计数
            try { incrementKeyCounter(client, "key.attack"); } catch (Throwable ignored) {}

            // 4. 原生攻击
            if (!isArmorStand) {
                try {
                    MappingHelper.invokeMethod(client, "doAttack");
                } catch (Throwable e) {
                    try {
                        java.lang.reflect.Method m = MappingHelper.findMethodByStructure(client.getClass(), null);
                        if (m != null && m.getName().startsWith("method_15")) { m.setAccessible(true); m.invoke(client); }
                    } catch (Throwable ignored) {}
                }
            }

            // 5. 领地标记 & 交互补偿
            if (im != null) {
                try {
                    Object blockPos = null;
                    Object side = null;

                    if (target != null && MappingHelper.getClass("BlockHitResult").isInstance(target)) {
                        blockPos = MappingHelper.invokeMethod(target, "getBlockPos");
                        side = MappingHelper.invokeMethod(target, "getSide");
                    } else if (isArmorStand && targetEntity != null) {
                        blockPos = MappingHelper.invokeMethod(targetEntity, "getBlockPos");
                        side = MappingHelper.getEnumConstant("Direction", "UP");
                    } else if (!isEntity) {
                        // 仅在非实体指向时尝试虚空标记
                        try {
                            Object cameraPos = MappingHelper.invokeMethod(player, "getCameraPosVec", 1.0f);
                            Object rotation = MappingHelper.invokeMethod(player, "getRotationVec", 1.0f);
                            if (cameraPos != null && rotation != null) {
                                double rx = ((Number) MappingHelper.getFieldValue(rotation, "x", null)).doubleValue();
                                double ry = ((Number) MappingHelper.getFieldValue(rotation, "y", null)).doubleValue();
                                double rz = ((Number) MappingHelper.getFieldValue(rotation, "z", null)).doubleValue();
                                double cx = ((Number) MappingHelper.getFieldValue(cameraPos, "x", null)).doubleValue();
                                double cy = ((Number) MappingHelper.getFieldValue(cameraPos, "y", null)).doubleValue();
                                double cz = ((Number) MappingHelper.getFieldValue(cameraPos, "z", null)).doubleValue();

                                double reach = 4.5;
                                int bx = (int) Math.floor(cx + rx * reach);
                                int by = (int) Math.floor(cy + ry * reach);
                                int bz = (int) Math.floor(cz + rz * reach);
                                blockPos = MappingHelper.getClass("BlockPos").getConstructor(int.class, int.class, int.class).newInstance(bx, by, bz);
                                side = MappingHelper.getEnumConstant("Direction", "UP");
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (blockPos != null && side != null) {
                        try {
                            MappingHelper.invokeMethod(im, "attackBlock", blockPos, side);
                        } catch (Throwable e) {
                            try {
                                java.lang.reflect.Method m = MappingHelper.findMethodByStructure(im.getClass(), null, blockPos.getClass(), side.getClass());
                                if (m != null) { m.setAccessible(true); m.invoke(im, blockPos, side); }
                            } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}
            }

            // 6. 实体补偿 (非盔甲架)
            if (!isArmorStand && targetEntity != null) {
                try {
                    MappingHelper.setFieldValue(targetEntity, "field_6008", 0);
                    MappingHelper.setFieldValue(targetEntity, "field_6007", 0);
                    MappingHelper.invokeMethod(im, "attackEntity", player, targetEntity);
                } catch (Throwable ignored) {}
            }

            // 7. 挥手动效 (兜底触发)
            if (mainHand != null) {
                try {
                    MappingHelper.invokeMethod(player, "swingHand", mainHand);
                } catch (Throwable e) {
                    try {
                        java.lang.reflect.Method m = MappingHelper.findMethodByStructure(player.getClass(), null, mainHand.getClass());
                        if (m != null) { m.setAccessible(true); m.invoke(player, mainHand); }
                    } catch (Throwable ignored) {}
                }
            }

            resetAttackCooldown(client);
        } catch (Throwable ignored) {}
    }

    private static void triggerItemUse(Object client, Object player) {
        try {
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");
            if (mainHand == null || im == null) return;

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

            // 4. 强制触发挥手
            boolean isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem");
            if (!isUsing) MappingHelper.invokeMethod(player, "swingHand", mainHand);
        } catch (Exception ignored) {}
    }


    private static void resetAttackCooldown(Object client) {
        try {
            // MinecraftClient.attackCooldown: field_1752 (1.21.1), field_1755 (1.21.4+)
            String[] fields = {"field_1752", "field1752", "field_1755", "field1755", "attackCooldown"};
            for (String f : fields) {
                try { MappingHelper.setFieldValue(client, f, 0); } catch (Exception ignored) {}
            }

            // 精准重置：仅针对疑似冷却的字段，避免误伤其他关键 int
            try {
                for (java.lang.reflect.Field f : client.getClass().getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        String name = f.getName();
                        if (name.contains("Cooldown") || name.equals("field_1752") || name.equals("field_1755") || name.equals("field_1753")) {
                            f.setAccessible(true);
                            f.setInt(client, 0);
                        }
                    }
                }
            } catch (Throwable ignored) {}

            Object player = CommandDispatcher.getClientPlayer();
            if (player != null) {
                // 强制重置玩家攻击强度计时器: field_6010 (lastAttackedTicks)
                try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}
                // 重置挥手进度: field_6013 (swingProgressInt)
                try { MappingHelper.setFieldValue(player, "field_6013", 0); } catch (Exception ignored) {}

                // 如果正在使用物品，强制停止
                boolean isUsing = false;
                try { isUsing = (boolean) MappingHelper.invokeMethod(player, "isUsingItem"); } catch (Exception ignored) {}
                if (isUsing) {
                    Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
                    if (im != null) {
                        try { MappingHelper.invokeMethod(im, "stopUsingItem", player); } catch (Exception ignored) {}
                        try { MappingHelper.invokeMethod(im, "method_2907", player); } catch (Exception ignored) {}
                    }
                }

                // 强制重置准星目标的无敌时间
                Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
                if (target != null && MappingHelper.getClass("EntityHitResult").isInstance(target)) {
                    Object entity = MappingHelper.invokeMethod(target, "getEntity");
                    if (entity != null) {
                        try { MappingHelper.setFieldValue(entity, "field_6008", 0); } catch (Exception ignored) {} // hurtResistantTime
                        try { MappingHelper.setFieldValue(entity, "field_6007", 0); } catch (Exception ignored) {} // hurtTime
                    }
                }
            }

            // 设置 ClientPlayerInteractionManager 中的各种延迟/冷却为 0
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            if (im != null) {
                String[] imFields = {"field_1613", "field1613", "field_1611", "field1611"}; // blockBreakingCooldown, etc.
                for (String f : imFields) {
                    try { MappingHelper.setFieldValue(im, f, 0); } catch (Exception ignored) {}
                }
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
