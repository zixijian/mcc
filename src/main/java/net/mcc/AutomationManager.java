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

            // 健壮的当前屏幕检测：任何屏幕打开时停止自动化 (包括聊天框)
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
                try { MappingHelper.setFieldValue(player, "field_6010", 200); } catch (Exception ignored) {}
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
            // 1. 攻击前置：彻底重置冷却与锁定满蓄力 (100) 以确保 100% 触发横扫和全额伤害
            resetAttackCooldown(client);
            try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}

            Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
            if (target == null) target = MappingHelper.findUniqueFieldByType(client, MappingHelper.getClass("net.minecraft.class_239"));

            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");

            // 2. 针对 1.21.11 的木斧标记补偿 (方块优先)
            Class<?> blockHitResultClass = null;
            try { blockHitResultClass = MappingHelper.getClass("BlockHitResult"); } catch (Exception ignored) {}
            if (target != null && blockHitResultClass != null && blockHitResultClass.isInstance(target)) {
                if (im != null) {
                    try {
                        Object pos = MappingHelper.invokeMethod(target, "getBlockPos");
                        Object side = MappingHelper.invokeMethod(target, "getSide");
                        if (pos != null && side != null) {
                            try {
                                MappingHelper.invokeMethod(im, "attackBlock", pos, side, 0); // 1.21.11/1.21.4 专用
                            } catch (Exception e) {
                                try { MappingHelper.invokeMethod(im, "attackBlock", pos, side); } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 3. 原生逻辑调用：触发挥手包和攻击事件
            try {
                MappingHelper.invokeMethod(client, "doAttack");
            } catch (Exception e) {
                try {
                    for (java.lang.reflect.Method m : client.getClass().getDeclaredMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().startsWith("method_15") || m.getName().equals("doAttack"))) {
                            m.setAccessible(true); m.invoke(client); break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // 4. 再次锁定蓄力：防止 doAttack 内部重置蓄力导致下一帧或当前帧补偿攻击变为弱攻击
            try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}

            // 5. 实体补偿 (确保每一帧高频攻击都能在服务端产生伤害效果)
            if (target != null) {
                Class<?> entityHitResultClass = null;
                try { entityHitResultClass = MappingHelper.getClass("EntityHitResult"); } catch (Exception ignored) {}
                if (entityHitResultClass != null && entityHitResultClass.isInstance(target)) {
                    Object entity = null;
                    try { entity = MappingHelper.invokeMethod(target, "getEntity"); } catch (Exception ignored) {}
                    if (entity == null) {
                        try { entity = MappingHelper.invokeMethod(target, "method_17770"); } catch (Exception ignored) {}
                    }
                    if (entity != null) {
                        // 强制重置目标无敌时间，确保每一发都有伤害
                        try { MappingHelper.setFieldValue(entity, "hurtResistantTime", 0); } catch (Exception ignored) {}
                        try { MappingHelper.setFieldValue(entity, "field_6008", 0); } catch (Exception ignored) {}
                        try { MappingHelper.setFieldValue(entity, "field_6007", 0); } catch (Exception ignored) {}

                        if (im != null) {
                            try {
                                MappingHelper.invokeMethod(im, "attackEntity", player, entity);
                            } catch (Exception e) {
                                // 结构化补偿
                                java.lang.reflect.Method m = MappingHelper.findMethodByStructure(im.getClass(), null, player.getClass(), entity.getClass());
                                if (m != null) { m.setAccessible(true); m.invoke(im, player, entity); }
                            }
                        }
                    }
                }
            }

            // 4. 强制视觉反馈：重置挥手动画以支持高频 (仅在需要时重置，防止动画过快看起来不顺畅)
            if (mainHand != null) {
                try {
                    boolean isSwinging = false;
                    try { isSwinging = (boolean) MappingHelper.getFieldValue(player, "handSwinging", null); } catch (Exception ignored) {}

                    if (isSwinging) {
                        try { MappingHelper.setFieldValue(player, "handSwinging", false); } catch (Exception ignored) {}
                        try { MappingHelper.setFieldValue(player, "field_6277", false); } catch (Exception ignored) {}
                        try { MappingHelper.setFieldValue(player, "handSwingTicks", 0); } catch (Exception ignored) {}
                        try { MappingHelper.setFieldValue(player, "field_6259", 0); } catch (Exception ignored) {}
                    }

                    MappingHelper.invokeMethod(player, "swingHand", mainHand);
                } catch (Exception e) {
                    // 暴力查找 swingHand (Hand)
                    try {
                        java.lang.reflect.Method m = MappingHelper.findMethodByStructure(player.getClass(), null, mainHand.getClass());
                        if (m != null) { m.setAccessible(true); m.invoke(player, mainHand); }
                    } catch (Exception ignored) {}
                }
            }

            // 攻击后再次确保冷却重置，防止原生逻辑在 doAttack 结尾重新设置冷却
            resetAttackCooldown(client);

        } catch (Exception ignored) {}
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

            Class<?> blockHitResultClass = null;
            try { blockHitResultClass = MappingHelper.getClass("BlockHitResult"); } catch (Exception ignored) {}
            if (target != null && blockHitResultClass != null && blockHitResultClass.isInstance(target)) {
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
                // 强制设为蓄力满状态: field_6010 (lastAttackedTicks)
                // 注意：不能调用 resetLastAttackedTicks()，因为那会把蓄力清零
                try { MappingHelper.setFieldValue(player, "field_6010", 100); } catch (Exception ignored) {}
                try { MappingHelper.setFieldValue(player, "lastAttackedTicks", 100); } catch (Exception ignored) {}

                // 停止使用物品（如果正在使用），防止阻塞攻击
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
                Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
                if (target != null) {
                    Class<?> entityHitResultClass = null;
                    try { entityHitResultClass = MappingHelper.getClass("EntityHitResult"); } catch (Exception ignored) {}
                    if (entityHitResultClass != null && entityHitResultClass.isInstance(target)) {
                        Object entity = MappingHelper.invokeMethod(target, "getEntity");
                        if (entity != null) {
                            try { MappingHelper.setFieldValue(entity, "field_6008", 0); } catch (Exception ignored) {} // hurtResistantTime
                            try { MappingHelper.setFieldValue(entity, "field_6007", 0); } catch (Exception ignored) {} // hurtTime
                            try { MappingHelper.setFieldValue(entity, "hurtResistantTime", 0); } catch (Exception ignored) {}
                            try { MappingHelper.setFieldValue(entity, "hurtTime", 0); } catch (Exception ignored) {}
                        }
                    }
                }
            }

            // 设置 ClientPlayerInteractionManager.blockBreakingCooldown 为 0
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            if (im != null) {
                try { MappingHelper.setFieldValue(im, "field_1613", 0); } catch (Exception ignored) {}
                try { MappingHelper.setFieldValue(im, "blockBreakingCooldown", 0); } catch (Exception ignored) {}
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
