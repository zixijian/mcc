package net.mcc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCCMod implements ClientModInitializer, ModInitializer {
    public static final String MOD_ID = "mcc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("MCC Mod Initialized (Client)");
    }

    @Override
    public void onInitialize() {
        LOGGER.info("MCC Mod Initialized (Common Events)");

        // 核心逻辑：确保攻击倍率满载且无敌帧移除
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player == null) return ActionResult.PASS;

            // 1. 强制满蓄力 (针对当前点击)
            try {
                MappingHelper.setFieldValue(player, "field_6010", 100);
            } catch (Throwable ignored) {}

            // 2. 延迟移除目标受击无敌时间 (确保当前攻击判定后再清零，供下一 tick 使用)
            if (entity != null) {
                // 如果在服务端环境，提交任务到服务器线程末尾执行
                if (!world.isClient()) {
                    try {
                        Object server = world.getClass().getMethod("getServer").invoke(world);
                        if (server != null) {
                            java.lang.reflect.Method execute = server.getClass().getMethod("execute", Runnable.class);
                            execute.invoke(server, (Runnable) () -> {
                                try {
                                    MappingHelper.setFieldValue(entity, "field_6008", 0);
                                    MappingHelper.setFieldValue(entity, "hurtResistantTime", 0);
                                } catch (Throwable ignored) {}
                            });
                        }
                    } catch (Throwable ignored) {}
                } else {
                    // 客户端环境直接标记 (用于预测)
                    try {
                        MappingHelper.setFieldValue(entity, "field_6008", 0);
                    } catch (Throwable ignored) {}
                }
            }

            return ActionResult.PASS;
        });

        // 针对方块标记的蓄力重置
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player != null) {
                try {
                    MappingHelper.setFieldValue(player, "field_6010", 100);
                } catch (Throwable ignored) {}
            }
            return ActionResult.PASS;
        });
    }
}
