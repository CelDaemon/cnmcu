package com.elmfer.cnmcu.client;

import com.elmfer.cnmcu.client.config.Config;
import com.elmfer.cnmcu.client.network.ClientNetworking;
import com.elmfer.cnmcu.client.screen.IDEScreen;
import com.elmfer.cnmcu.client.toolchain.ClientModSetup;
import com.elmfer.cnmcu.client.toolchain.Toolchain;
import com.elmfer.cnmcu.menu.Menus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class ClientInitialiser implements ClientModInitializer {
    public static final Config CONFIG = new Config();
    public static final Toolchain TOOLCHAIN = new Toolchain(CONFIG.getToolchainConfig());

    @Override
    public void onInitializeClient() {
        CONFIG.read();

        ClientModSetup.createDirectories();
        ClientModSetup.downloadToolchain();

        MenuScreens.register(Menus.IDE_MENU, IDEScreen::new);

        ClientNetworking.register();

        EventHandler.register();
    }
}
