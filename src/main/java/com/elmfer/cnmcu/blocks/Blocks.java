package com.elmfer.cnmcu.blocks;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class Blocks {
    public static final CNnanoBlock CN_NANO_BLOCK = new CNnanoBlock(AbstractBlock.Settings.create().strength(0.5f));

    public static <T extends Block> T register(String name, T block) {
        T b = Registry.register(Registries.BLOCK, CodeNodeMicrocontrollers.id(name), block);
        BlockItem item = new BlockItem(b, new Item.Settings());
        item.appendBlocks(Item.BLOCK_ITEMS, item);
        Registry.register(Registries.ITEM, CodeNodeMicrocontrollers.id(name), item);
        return b;
    }

    public static void init() {
        register("nano", CN_NANO_BLOCK);
    }
}
