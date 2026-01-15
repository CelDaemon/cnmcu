package com.elmfer.cnmcu;

import com.elmfer.cnmcu.config.ModSetup;
import com.elmfer.cnmcu.network.Packets;
import com.elmfer.cnmcu.ui.IDEScreen;
import com.elmfer.cnmcu.ui.menu.Menus;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class CodeNodeMicrocontrollersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModSetup.imguiIniFile();
        
        MenuScreens.register(Menus.IDE_MENU, IDEScreen::new);

        Packets.initClientPackets();
        
        EventHandler.registerClientEventHandlers();
    }
}
