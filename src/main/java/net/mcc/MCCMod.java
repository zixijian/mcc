package net.mcc;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MCCMod implements ClientModInitializer {
    public static final String MOD_ID = "mcc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("MCC Mod Initialized (Zero-Link Mode)");
        // 移除对 Fabric API 事件的直接调用，改用 Mixin 驱动 Ticking
    }
}
