package com.elmfer.cnmcu.blockentities;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blocks.Blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntities {
    public static void init() {
        // DUMMY
    }
    
    public static final BlockEntityType<CNnanoBlockEntity> CN_NANO = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CodeNodeMicrocontrollers.id("nano"),
            FabricBlockEntityTypeBuilder.create(CNnanoBlockEntity::new, Blocks.CN_NANO_BLOCK).build());
}
