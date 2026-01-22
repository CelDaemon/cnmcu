package com.elmfer.cnmcu.datagen;

import com.elmfer.cnmcu.DataComponents;
import com.elmfer.cnmcu.blocks.Blocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;

import java.util.concurrent.CompletableFuture;

public class CNMCUBlockLootTableProvider extends FabricBlockLootTableProvider {

    protected CNMCUBlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    public LootTable.Builder createNanoDrops() {
        final var item = LootItem.lootTableItem(Blocks.CN_NANO_BLOCK)
                .apply(
                        CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                .include(DataComponents.CODE)
                );
        return LootTable.lootTable()
                .withPool(
                        LootPool.lootPool()
                                .add(item)
                                .when(ExplosionCondition.survivesExplosion())
                );
    }
    @Override
    public void generate() {
        add(Blocks.CN_NANO_BLOCK, createNanoDrops());
    }
}
