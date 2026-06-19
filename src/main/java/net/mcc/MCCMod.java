package net.mcc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核心入口类。
 * 使用反射与 Fabric 事件系统挂钩，完全隔离 Minecraft 类以防止类加载崩溃。
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
        LOGGER.info("MCC Mod Common Initialized");
        try {
            registerAttackEvents();
        } catch (Throwable e) {
            LOGGER.error("Failed to register attack events: " + e.toString());
        }
    }

    private void registerAttackEvents() throws Exception {
        // 动态定位 Fabric API 事件类
        Class<?> attackEntityCallback = Class.forName("net.fabricmc.fabric.api.event.player.AttackEntityCallback");
        Class<?> attackBlockCallback = Class.forName("net.fabricmc.fabric.api.event.player.AttackBlockCallback");

        // 1. 注册 AttackEntityCallback (参数接口本身就是回调类)
        registerFabricEvent(attackEntityCallback, (proxy, method, args) -> {
            // Callback 方法名通常为 interact
            if (method.getName().equals("interact")) {
                Object player = args[0];
                Object world = args[1];
                Object entity = args[3];
                if (player != null) {
                    try {
                        // 锁定满蓄力 (field_6010 = 100)
                        MappingHelper.setFieldValue(player, "field_6010", 100);
                    } catch (Throwable ignored) {}

                    // 移除目标受击无敌帧 (实现高频伤害)
                    if (entity != null) {
                        try {
                            boolean isClient = true;
                            // 探测是否为客户端
                            try {
                                Object isClientObj = MappingHelper.getFieldValue(world, "isClient", null);
                                if (isClientObj instanceof Boolean) isClient = (Boolean) isClientObj;
                            } catch (Exception e) {
                                try { isClient = (boolean) MappingHelper.invokeMethod(world, "isClient"); } catch (Exception ignored2) {}
                            }

                            if (!isClient) {
                                // 服务端逻辑：延迟到 tick 末尾清除，确保当前攻击判定完成
                                Object server = null;
                                try { server = MappingHelper.invokeMethod(world, "getServer"); } catch (Exception ignored2) {}
                                if (server != null) {
                                    final Object targetEntity = entity;
                                    java.lang.reflect.Method execute = server.getClass().getMethod("execute", Runnable.class);
                                    execute.invoke(server, (Runnable) () -> {
                                        clearInvulnerability(targetEntity);
                                    });
                                }
                            } else {
                                // 客户端逻辑：直接清除以支持预测
                                clearInvulnerability(entity);
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
                    } catch (Throwable ignored) {}
                }
            }
            return ActionResultStatic.PASS();
        });
    }

    private void registerFabricEvent(Class<?> eventClass, java.lang.reflect.InvocationHandler handler) throws Exception {
        java.lang.reflect.Field eventField = eventClass.getField("EVENT");
        Object eventInstance = eventField.get(null);
        // 动态代理注册
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{eventClass}, handler);
        eventInstance.getClass().getMethod("register", eventClass).invoke(eventInstance, proxy);
    }

    private static void clearInvulnerability(Object entity) {
        try {
            MappingHelper.setFieldValue(entity, "field_6008", 0); // hurtResistantTime
            MappingHelper.setFieldValue(entity, "hurtResistantTime", 0);
            MappingHelper.setFieldValue(entity, "field_6007", 0); // hurtTime
            MappingHelper.setFieldValue(entity, "hurtTime", 0);
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
