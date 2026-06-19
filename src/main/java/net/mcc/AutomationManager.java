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
            attackTimer = 0;
            CommandDispatcher.addFeedback("§a执行攻击一次");
            return;
        }
        attackFreq = freq;
        attackTimer = 0;
        if (freq >= 0) {
            useFreq = -1;
            try { releaseKeyTranslation(CommandDispatcher.getClient(), "key.use"); } catch (Exception ignored) {}
        } else {
            try { releaseKeyTranslation(CommandDispatcher.getClient(), "key.attack"); } catch (Exception ignored) {}
        }
        CommandDispatcher.addFeedback("§a自动攻击: " + (freq == -1 ? "关闭" : (freq == 0 ? "持续" : freq + " ticks")));
    }

    public static void setUse(int freq, boolean hasArgs) {
        if (!hasArgs) {
            useOnce = true;
            useTimer = 0;
            CommandDispatcher.addFeedback("§a执行使用一次");
            return;
        }
        useFreq = freq;
        useTimer = 0;
        if (freq >= 0) {
            attackFreq = -1;
            try { releaseKeyTranslation(CommandDispatcher.getClient(), "key.attack"); } catch (Exception ignored) {}
        } else {
            try { releaseKeyTranslation(CommandDispatcher.getClient(), "key.use"); } catch (Exception ignored) {}
        }
        CommandDispatcher.addFeedback("§a自动使用: " + (freq == -1 ? "关闭" : (freq == 0 ? "持续" : freq + " ticks")));
    }

    public static void toggleAutoRespawn() {
        autoRespawn = !autoRespawn;
        CommandDispatcher.addFeedback("§a自动复活: " + (autoRespawn ? "开启" : "关闭"));
    }

    public static void setLook(float pitch, float yaw) throws Exception {
        lockedPitch = pitch; lockedYaw = yaw;
        Object player = CommandDispatcher.getClientPlayer();
        if (player != null) {
            MappingHelper.invokeMethod(player, "setYaw", yaw);
            MappingHelper.invokeMethod(player, "setPitch", pitch);
            CommandDispatcher.addFeedback(String.format("§a设定朝向: Pitch %.1f, Yaw %.1f", pitch, yaw));
        }
    }

    public static void showLook() throws Exception {
        Object player = CommandDispatcher.getClientPlayer();
        if (player != null) {
            float yaw = ((Number) MappingHelper.invokeMethod(player, "getYaw")).floatValue();
            float pitch = ((Number) MappingHelper.invokeMethod(player, "getPitch")).floatValue();
            CommandDispatcher.addFeedback(String.format("§e当前朝向: Pitch %.1f, Yaw %.1f", pitch, yaw));
        }
    }

    public static void stopAll() {
        attackFreq = -1; useFreq = -1; attackOnce = useOnce = false;
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
            if (client == null) return;

            String[] atkCooldowns = {"field_1752", "field_1755", "attackCooldown"};
            for (String f : atkCooldowns) {
                try {
                    Object val = MappingHelper.getFieldValue(client, f, null);
                    CommandDispatcher.addFeedback("§a- AtkCooldown field " + f + " value: " + val);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            CommandDispatcher.addFeedback("§cProbe failed: " + e.toString());
        }
    }

    /**
     * 每 Tick 回调
     */
    public static void onClientTick() {
        try {
            Object client = CommandDispatcher.getClient();
            Object player = CommandDispatcher.getClientPlayer();
            if (player == null) return;

            Object currentScreen = null;
            try {
                Class<?> screenClass = MappingHelper.getClass("Screen");
                currentScreen = MappingHelper.findUniqueFieldByType(client, screenClass);
                if (currentScreen != null) {
                    if (MappingHelper.getClass("ChatScreen").isInstance(currentScreen)) currentScreen = null;
                }
            } catch (Exception ignored) {}
            if (currentScreen != null) return;

            if (lockedPitch != null && lockedYaw != null) {
                try {
                    MappingHelper.invokeMethod(player, "setYaw", lockedYaw);
                    MappingHelper.invokeMethod(player, "setPitch", lockedPitch);
                } catch (Exception ignored) {}
            }

            if (autoRespawn) {
                float hp = ((Number) MappingHelper.invokeMethod(player, "getHealth")).floatValue();
                if (hp <= 0) MappingHelper.invokeMethod(player, "requestRespawn");
            }

            if (attackFreq >= 0 || attackOnce) {
                try {
                    MappingHelper.setFieldValue(player, "field_6010", 100);
                    MappingHelper.setFieldValue(player, "lastAttackedTicks", 100);
                } catch (Exception ignored) {}
            }

            if (attackOnce) {
                triggerAttack(client, player); attackOnce = false;
            } else if (attackFreq == 0) {
                triggerAttack(client, player);
            } else if (attackFreq > 0) {
                if (--attackTimer <= 0) {
                    triggerAttack(client, player); attackTimer = attackFreq;
                }
            }

            if (useOnce) {
                resetUseCooldown(client); triggerItemUse(client, player); useOnce = false;
            } else if (useFreq == 0) {
                resetUseCooldown(client); triggerItemUse(client, player);
            } else if (useFreq > 0) {
                if (--useTimer <= 0) {
                    resetUseCooldown(client); triggerItemUse(client, player); useTimer = useFreq;
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void triggerAttack(Object client, Object player) {
        try {
            resetAttackCooldown(client);
            try {
                MappingHelper.setFieldValue(player, "field_6010", 100);
                MappingHelper.setFieldValue(player, "lastAttackedTicks", 100);
            } catch (Exception ignored) {}

            Object target = MappingHelper.getFieldValue(client, "crosshairTarget", null);
            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            Object mainHand = MappingHelper.getEnumConstant("Hand", "MAIN_HAND");

            boolean isBlock = false;
            try {
                Class<?> blockHitResultClass = MappingHelper.getClass("BlockHitResult");
                if (target != null && blockHitResultClass.isInstance(target)) {
                    isBlock = true;
                    if (im != null) {
                        Object pos = MappingHelper.invokeMethod(target, "getBlockPos");
                        Object side = MappingHelper.invokeMethod(target, "getSide");
                        if (pos != null && side != null) {
                            try { MappingHelper.invokeMethod(im, "attackBlock", pos, side, 0); } catch (Exception e) {
                                try { MappingHelper.invokeMethod(im, "attackBlock", pos, side); } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            // doAttack 处理原生挥手和实体攻击
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

            try {
                MappingHelper.setFieldValue(player, "field_6010", 100);
                MappingHelper.setFieldValue(player, "lastAttackedTicks", 100);
            } catch (Exception ignored) {}

            // 实体无敌帧清除
            if (!isBlock && target != null) {
                try {
                    Class<?> entityHitResultClass = MappingHelper.getClass("EntityHitResult");
                    if (entityHitResultClass.isInstance(target)) {
                        Object entity = MappingHelper.invokeMethod(target, "getEntity");
                        if (entity != null) {
                            MappingHelper.setFieldValue(entity, "field_6008", 0);
                            MappingHelper.setFieldValue(entity, "field_6007", 0);
                            if (im != null) {
                                try { MappingHelper.invokeMethod(im, "attackEntity", player, entity); } catch (Exception ignored) {}
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (mainHand != null) {
                try {
                    try { MappingHelper.setFieldValue(player, "handSwinging", false); } catch (Exception ignored) {}
                    try { MappingHelper.setFieldValue(player, "handSwingTicks", 0); } catch (Exception ignored) {}
                    MappingHelper.invokeMethod(player, "swingHand", mainHand);
                } catch (Exception ignored) {}
            }

            resetAttackCooldown(client);

        } catch (Exception ignored) {}
    }

    private static void triggerItemUse(Object client, Object player) {
        try {
            MappingHelper.invokeMethod(client, "doItemUse");
        } catch (Exception ignored) {}
    }

    private static void resetAttackCooldown(Object client) {
        try {
            String[] fields = {"field_1752", "field_1755", "attackCooldown"};
            for (String f : fields) { try { MappingHelper.setFieldValue(client, f, 0); } catch (Exception ignored) {} }

            Object im = MappingHelper.getFieldValue(client, "interactionManager", null);
            if (im != null) {
                try { MappingHelper.setFieldValue(im, "field_1613", 0); } catch (Exception ignored) {}
                try { MappingHelper.setFieldValue(im, "field_1612", 0); } catch (Exception ignored) {}
                try { MappingHelper.setFieldValue(im, "blockBreakingCooldown", 0); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void resetUseCooldown(Object client) {
        try {
            String[] fields = {"field_1753", "field_1752", "itemUseCooldown"};
            for (String f : fields) { try { MappingHelper.setFieldValue(client, f, 0); } catch (Exception ignored) {} }
        } catch (Exception ignored) {}
    }

    private static void releaseKeyTranslation(Object client, String translationKey) throws Exception {
        Object kb = findKeyBinding(client, translationKey);
        if (kb != null) {
            MappingHelper.setFieldValue(kb, "pressed", false);
            try { MappingHelper.setFieldValue(kb, "field_1652", 0); } catch (Exception ignored) {}
        }
    }

    private static void pressKeyTranslation(Object client, String translationKey) throws Exception {
        Object kb = findKeyBinding(client, translationKey);
        if (kb != null) MappingHelper.setFieldValue(kb, "pressed", true);
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
        for (java.lang.reflect.Field f : options.getClass().getDeclaredFields()) {
            if (kbClass.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object kb = f.get(options);
                if (kb != null) {
                    String tk = (String) MappingHelper.getFieldValue(kb, "translationKey", kbClass);
                    if (translationKey.equals(tk)) return kb;
                }
            }
        }
        return null;
    }
}
