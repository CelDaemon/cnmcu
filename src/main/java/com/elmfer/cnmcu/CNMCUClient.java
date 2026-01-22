package com.elmfer.cnmcu;

import com.elmfer.cnmcu.network.Packets;
import com.elmfer.cnmcu.ui.IDEScreen;
import com.elmfer.cnmcu.ui.menu.Menus;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class CNMCUClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Menus.IDE_MENU, IDEScreen::new);

        Packets.initClientPackets();
        
        EventHandler.registerClientEventHandlers();
    }
}
