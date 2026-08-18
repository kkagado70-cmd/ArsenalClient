package com.example.automace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoMaceMod implements ModInitializer {
    public static final String MOD_ID = "automace";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    private static KeyBinding guiKeyBinding;

    @Override
    public void onInitialize() {
        LOGGER.info("AutoMace standalone Fabric initialized for Kovak.");
        AutoMaceLogic.init();

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.automace.gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.automace.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AutoMaceLogic.onTick();
            
            while (guiKeyBinding.wasPressed()) {
                client.setScreen(new AutoMaceScreen());
            }
        });
    }
}