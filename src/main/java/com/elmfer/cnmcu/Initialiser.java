package com.elmfer.cnmcu;

import com.elmfer.cnmcu.blockentities.BlockEntities;
import com.elmfer.cnmcu.blocks.Blocks;
import com.elmfer.cnmcu.common.Common;
import com.elmfer.cnmcu.menu.Menus;
import com.elmfer.cnmcu.natives.NativesLoader;
import com.elmfer.cnmcu.network.Networking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.ref.Cleaner;
import java.nio.file.Path;

public class Initialiser implements ModInitializer {

    public static final String MOD_NAME;
    public static final String MOD_VERSION;
    public static final Path DATA_PATH = FabricLoader.getInstance().getGameDir().resolve(Common.MOD_ID);
    public static final Cleaner CLEANER = Cleaner.create();

    static {
        var metadata = FabricLoader.getInstance().getModContainer(Common.MOD_ID).orElseThrow().getMetadata();
        MOD_VERSION = metadata.getVersion().getFriendlyString();
        MOD_NAME = metadata.getName();
    }



    @Override
    public void onInitialize() {
        NativesLoader.loadNatives();

        Blocks.init();
        BlockEntities.init();
        Menus.init();
        DataComponents.init();

        Networking.register();
    }

}