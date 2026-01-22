package com.elmfer.cnmcu.blockentities;

import com.elmfer.cnmcu.CNMCU;
import com.elmfer.cnmcu.blocks.Blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntities {
    public static void init() {
        // DUMMY
    }
    
    public static final BlockEntityType<NanoBlockEntity> CN_NANO = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CNMCU.id("nano"),
            FabricBlockEntityTypeBuilder.create(NanoBlockEntity::new, Blocks.CN_NANO_BLOCK).build());
}
