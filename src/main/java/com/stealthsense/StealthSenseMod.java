package com.stealthsense;

import com.stealthsense.config.StealthConfig;
import com.stealthsense.logic.DetectionEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StealthSenseMod implements ModInitializer {
    public static final String MOD_ID = "stealthsense";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        StealthConfig.load();
        ServerTickEvents.END_LEVEL_TICK.register(DetectionEngine::tickWorld);
        LOGGER.info("StealthSense v0.1.0 initialized for Minecraft 26.3!");
    }
}
