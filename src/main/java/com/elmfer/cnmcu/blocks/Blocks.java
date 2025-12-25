package com.elmfer.cnmcu.blocks;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.util.function.Function;

public class Blocks {
    public static final CNnanoBlock CN_NANO_BLOCK = register("nano", CNnanoBlock::new, AbstractBlock.Settings.create().strength(0.5f));

    public static <T extends Block> T register(String name, Function<AbstractBlock.Settings, T> blockFactory, AbstractBlock.Settings blockSettings) {
        var blockKey = RegistryKey.of(Registries.BLOCK.getKey(), CodeNodeMicrocontrollers.id(name));
        var block = blockFactory.apply(blockSettings.registryKey(blockKey));
        var itemKey = RegistryKey.of(Registries.ITEM.getKey(), CodeNodeMicrocontrollers.id(name));
        var blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, itemKey, blockItem);
        Registry.register(Registries.BLOCK, blockKey, block);

        return block;
    }

    public static void init() {
        // DUMMY
    }
}
