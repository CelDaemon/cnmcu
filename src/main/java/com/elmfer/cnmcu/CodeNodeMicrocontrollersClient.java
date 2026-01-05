package com.elmfer.cnmcu;

import com.elmfer.cnmcu.config.ModSetup;
import com.elmfer.cnmcu.network.Packets;
import com.elmfer.cnmcu.ui.IDEScreen;
import com.elmfer.cnmcu.ui.handler.ScreenHandlers;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class CodeNodeMicrocontrollersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModSetup.imguiIniFile();
        
        MenuScreens.register(ScreenHandlers.IDE_SCREEN_HANDLER, IDEScreen::new);

        Packets.initClientPackets();
        
        EventHandler.registerClientEventHandlers();
    }
}
