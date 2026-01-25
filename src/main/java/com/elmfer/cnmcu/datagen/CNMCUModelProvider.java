package com.elmfer.cnmcu.datagen;

import com.elmfer.cnmcu.blocks.Blocks;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

public class CNMCUModelProvider extends FabricModelProvider {
    public CNMCUModelProvider(FabricDataOutput output) {
        super(output);
    }

    private static final PropertyDispatch<VariantMutator> ROTATION_HORIZONTAL_FACING_ALT = PropertyDispatch.modify(BlockStateProperties.HORIZONTAL_FACING)
            .select(Direction.SOUTH, BlockModelGenerators.NOP)
            .select(Direction.WEST, BlockModelGenerators.Y_ROT_90)
            .select(Direction.NORTH, BlockModelGenerators.Y_ROT_180)
            .select(Direction.EAST, BlockModelGenerators.Y_ROT_270);

    @Override
    public void generateBlockStateModels(@NotNull BlockModelGenerators blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleFlatItemModel(Blocks.NANO_BLOCK);
        blockStateModelGenerator.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(Blocks.NANO_BLOCK,
                        BlockModelGenerators.plainVariant(ModelLocationUtils.getModelLocation(Blocks.NANO_BLOCK))
                ).with(ROTATION_HORIZONTAL_FACING_ALT)
        );
    }

    @Override
    public void generateItemModels(@NotNull ItemModelGenerators itemModelGenerator) {

    }
}
