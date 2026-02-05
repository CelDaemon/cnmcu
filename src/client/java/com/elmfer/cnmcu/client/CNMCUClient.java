package com.elmfer.cnmcu.client;

import com.elmfer.cnmcu.client.network.ClientNetworking;
import com.elmfer.cnmcu.client.screen.IDEScreen;
import com.elmfer.cnmcu.client.toolchain.ClientModSetup;
import com.elmfer.cnmcu.client.toolchain.Config;
import com.elmfer.cnmcu.client.toolchain.Toolchain;
import com.elmfer.cnmcu.menu.Menus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class CNMCUClient implements ClientModInitializer {
    public static final Config CONFIG = Config.load();
    public static final Toolchain TOOLCHAIN = new Toolchain();

    @Override
    public void onInitializeClient() {
        ClientModSetup.createDirectories();
        ClientModSetup.downloadToolchain();

        MenuScreens.register(Menus.IDE_MENU, IDEScreen::new);

        ClientNetworking.register();

        EventHandler.register();
    }
}
