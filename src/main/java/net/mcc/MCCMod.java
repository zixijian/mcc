package net.mcc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 核心入口类。
 * 采用反射驱动型架构，确保 1.21.x 全版本兼容性并防止类加载崩溃。
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
        LOGGER.info("MCC Mod Common Initialized (Universal Reflective Mode)");
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

        // 1. 注册 AttackEntityCallback (处理蓄力锁定与无敌帧)
        registerFabricEvent(attackEntityCallback, (proxy, method, args) -> {
            if (method.getName().equals("interact")) {
                Object player = args[0];
                Object world = args[1];
                Object entity = args[3];
                if (player != null) {
                    // 攻击判定瞬间：锁定满蓄力 (100)
                    try {
                        MappingHelper.setFieldValue(player, "field_6010", 100);
                        MappingHelper.setFieldValue(player, "lastAttackedTicks", 100);
                    } catch (Throwable ignored) {}

                    // 目标处理：清除无敌帧实现高频伤害
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
                                // 服务端：在服务器线程末尾清除，确保当前 tick 的伤害判定已正常结束
                                Object server = MappingHelper.invokeMethod(world, "getServer");
                                if (server != null) {
                                    final Object targetEntity = entity;
                                    java.lang.reflect.Method execute = server.getClass().getMethod("execute", Runnable.class);
                                    execute.invoke(server, (Runnable) () -> forceClearInvulnerability(targetEntity));
                                }
                            } else {
                                // 客户端：立即清除以支持预测逻辑
                                forceClearInvulnerability(entity);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
            return ActionResultStatic.PASS();
        });

        // 2. 注册 AttackBlockCallback (针对方块标记，同样锁定蓄力)
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
        // 使用动态代理，直接使用 Callback 接口类作为代理接口
        Object proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{eventClass}, handler);
        eventInstance.getClass().getMethod("register", eventClass).invoke(eventInstance, proxy);
    }

    /**
     * 彻底清除实体的受击计时器，确保高频有效
     */
    private static void forceClearInvulnerability(Object entity) {
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
