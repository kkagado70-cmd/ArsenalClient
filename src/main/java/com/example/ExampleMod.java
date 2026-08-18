package com.example.automace;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoMaceMod implements ModInitializer {
    public static final String MOD_ID = "automace";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("AutoMace standalone Fabric initialized for Kovak.");
        AutoMaceLogic.init();
    }
}