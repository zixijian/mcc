package net.mcc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核心入口类。
 * 采用反射驱动型架构，确保 1.21.x 全版本兼容。
 */
public class MCCMod implements ClientModInitializer, ModInitializer {
    public static final String MOD_ID = "mcc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("MCC Mod Client Initialized");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("MCC Mod Common Initialized (Reflective Mode)");
        try {
            registerAttackEvents();
        } catch (Throwable e) {
            LOGGER.error("Failed to register attack events: " + e.toString());
        }
    }

    private void registerAttackEvents() throws Exception {
        Class<?> attackEntityCallback = Class.forName("net.fabricmc.fabric.api.event.player.AttackEntityCallback");
        Class<?> attackBlockCallback = Class.forName("net.fabricmc.fabric.api.event.player.AttackBlockCallback");

        // 1. 注册 AttackEntityCallback
        registerFabricEvent(attackEntityCallback, (proxy, method, args) -> {
            if (method.getName().equals("interact")) {
                Object player = args[0];
                Object world = args[1];
                Object entity = args[3];
                if (player != null) {
                    // 攻击判定瞬间：锁定满蓄力 (field_6010 = 100)
                    try {
                        MappingHelper.setFieldValue(player, "field_6010", 100);
                        MappingHelper.setFieldValue(player, "lastAttackedTicks", 100);
                    } catch (Throwable ignored) {}

                    // 目标处理：清除无敌帧与受击计时器，确保 20Hz 伤害
                    if (entity != null) {
                        try {
                            boolean isClient = true;
                            Object isClientObj = null;
                            try { isClientObj = MappingHelper.getFieldValue(world, "field_9236", null); } catch (Exception ignored) {}
                            if (isClientObj instanceof Boolean) isClient = (Boolean) isClientObj;
                            else {
                                try { isClient = (boolean) MappingHelper.invokeMethod(world, "isClient"); } catch (Exception ignored2) {}
                            }

                            if (!isClient) {
                                // 服务端：在 tick 结束后清除，保证当前攻击伤害正常结算
                                Object server = MappingHelper.invokeMethod(world, "getServer");
                                if (server != null) {
                                    final Object targetEntity = entity;
                                    java.lang.reflect.Method execute = server.getClass().getMethod("execute", Runnable.class);
                                    execute.invoke(server, (Runnable) () -> forceClearInvulnerability(targetEntity));
                                }
                            } else {
                                // 客户端：立即清除支持预测
                                forceClearInvulnerability(entity);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
            return ActionResultStatic.PASS();
        });

        // 2. 注册 AttackBlockCallback
        registerFabricEvent(attackBlockCallback, (proxy, method, args) -> {
            if (method.getName().equals("interact")) {
                Object player = args[0];
                if (player != null) {
                    try {
                        MappingHelper.setFieldValue(player, "field_6010", 100);
                        MappingHelper.setFieldValue(player, "lastAttackedTicks", 100);
                    } catch (Throwable ignored) {}
                }
            }
            return ActionResultStatic.PASS();
        });
    }

    private void registerFabricEvent(Class<?> eventClass, java.lang.reflect.InvocationHandler handler) throws Exception {
        java.lang.reflect.Field eventField = eventClass.getField("EVENT");
        Object eventInstance = eventField.get(null);
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{eventClass}, handler);
        eventInstance.getClass().getMethod("register", eventClass).invoke(eventInstance, proxy);
    }

    private static void forceClearInvulnerability(Object entity) {
        try {
            // 彻底清除所有受击计时器
            MappingHelper.setFieldValue(entity, "field_6008", 0); // hurtResistantTime
            MappingHelper.setFieldValue(entity, "field_6007", 0); // hurtTime
            try { MappingHelper.setFieldValue(entity, "hurtResistantTime", 0); } catch (Throwable ignored) {}
            try { MappingHelper.setFieldValue(entity, "hurtTime", 0); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    private static class ActionResultStatic {
        private static Object pass = null;
        public static Object PASS() {
            if (pass == null) pass = MappingHelper.getEnumConstant("net.minecraft.class_1269", "field_5811");
            if (pass == null) pass = MappingHelper.getEnumConstant("net.minecraft.class_1269", "PASS");
            return pass;
        }
    }
}
