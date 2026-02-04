package com.elmfer.cnmcu.client;

import com.elmfer.cnmcu.menu.Menus;
import com.elmfer.cnmcu.client.network.ClientNetworking;
import com.elmfer.cnmcu.client.screen.IDEScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class CNMCUClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Menus.IDE_MENU, IDEScreen::new);

        ClientNetworking.register();
        
        EventHandler.registerClientEventHandlers();
    }
}
