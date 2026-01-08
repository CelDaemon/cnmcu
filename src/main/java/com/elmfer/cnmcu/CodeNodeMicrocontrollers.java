package com.elmfer.cnmcu;

import com.elmfer.cnmcu.blockentities.BlockEntities;
import com.elmfer.cnmcu.blocks.Blocks;
import com.elmfer.cnmcu.config.Config;
import com.elmfer.cnmcu.config.ModSetup;
import com.elmfer.cnmcu.cpp.NativesLoader;
import com.elmfer.cnmcu.network.Packets;
import com.elmfer.cnmcu.ui.handler.Menus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Cleaner;

public class CodeNodeMicrocontrollers implements ModInitializer {

    public static final String MOD_ID = "cnmcu";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MOD_NAME;
    public static final String MOD_VERSION;
    public static final Cleaner CLEANER = Cleaner.create();
    static {
        var metadata = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
        MOD_VERSION = metadata.getVersion().getFriendlyString();
        MOD_NAME = metadata.getName();
    }

    @Override
    public void onInitialize() {

        ModSetup.createDirectories();
        ModSetup.downloadNatives();
        ModSetup.downloadToolchain();

        checkForUpdates();

        NativesLoader.loadNatives();

        Blocks.init();
        BlockEntities.init();
        Menus.init();
        DataComponents.init();

        Packets.registerPackets();
        Packets.initServerPackets();
    }

    public static void checkForUpdates() {
        if (!Config.adviseUpdates())
            return;

        ModSetup.checkForUpdates();

        if (ModSetup.isUpdateAvailable()) {
            var latestForMCVersions = String.join(", ", ModSetup.getLatestForMinecraftVersions());
            LOGGER.info("An update is available for CodeNode Microcontrollers: {} for Minecraft {}",
                    ModSetup.getLatestVersion(), latestForMCVersions);
        } else if (!ModSetup.wasAbleToCheckForUpdates()) {
            LOGGER.info("CodeNode Microcontrollers was unable to check for updates.");
        } else {
            LOGGER.info("CodeNode Microcontrollers is up to date.");
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}