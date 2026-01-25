package com.elmfer.cnmcu.blocks;

import com.elmfer.cnmcu.CNMCU;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public class Blocks {
    public static final NanoBlock NANO_BLOCK = register("nano", NanoBlock::new,
            BlockBehaviour.Properties.of().instabreak().sound(SoundType.STONE).pushReaction(PushReaction.DESTROY)
    );
    public static final ResourceKey<CreativeModeTab> CNMCU_TAB_KEY = ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), CNMCU.id("cnmcu"));

    static {
        register(
                CNMCU_TAB_KEY,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(NANO_BLOCK))
                        .displayItems((params, output) -> {
                            output.accept(NANO_BLOCK);
                        })
        );
    }


    public static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> blockFactory, BlockBehaviour.Properties blockSettings) {
        var blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), CNMCU.id(name));
        var block = blockFactory.apply(blockSettings.setId(blockKey));
        var itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), CNMCU.id(name));
        var blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        return block;
    }

    public static void register(ResourceKey<CreativeModeTab> key, CreativeModeTab.Builder tabBuilder) {
        final var tab = tabBuilder.title(
                        Component.translatable(key.identifier().toLanguageKey("itemGroup"))
                )
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, key, tab);
    }

    public static void init() {
        // DUMMY
    }
}
