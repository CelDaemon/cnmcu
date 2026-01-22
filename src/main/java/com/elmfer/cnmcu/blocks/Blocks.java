package com.elmfer.cnmcu.blocks;

import com.elmfer.cnmcu.CNMCU;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public class Blocks {
    public static final NanoBlock CN_NANO_BLOCK = register("nano", NanoBlock::new,
            BlockBehaviour.Properties.of().instabreak().sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
    );

    public static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> blockFactory, BlockBehaviour.Properties blockSettings) {
        var blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), CNMCU.id(name));
        var block = blockFactory.apply(blockSettings.setId(blockKey));
        var itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), CNMCU.id(name));
        var blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        return block;
    }

    public static void init() {
        // DUMMY
    }
}
