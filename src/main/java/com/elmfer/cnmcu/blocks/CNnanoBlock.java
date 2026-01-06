package com.elmfer.cnmcu.blocks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.elmfer.cnmcu.blockentities.BlockEntities;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CNnanoBlock extends BaseEntityBlock {
    public static final MapCodec<CNnanoBlock> CODEC = simpleCodec(CNnanoBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected CNnanoBlock(Properties settings) {
        super(settings);
    }

    @Override
    @NotNull
    public MapCodec<CNnanoBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CNnanoBlockEntity(pos, state);
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader world, @NotNull BlockPos pos) {
        BlockPos blockPos = pos.below();
        return this.canPlaceAbove(world, blockPos, world.getBlockState(blockPos));
    }

    protected boolean canPlaceAbove(LevelReader world, BlockPos pos, BlockState state) {
        return state.isFaceSturdy(world, pos, Direction.UP, SupportType.RIGID);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean isSignalSource(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getSignal(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull Direction direction) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof CNnanoBlockEntity entity))
            return 0;

        if (entity.mcu == null || !entity.mcu.isPowered())
            return 0;
        
        Direction blockDir = state.getValue(FACING);
        Direction localDir = getLocalDirection(blockDir, direction);

        return switch (localDir) {
            case EAST -> entity.mcu.rightOutput;
            case SOUTH -> entity.mcu.backOutput;
            case WEST -> entity.mcu.leftOutput;
            case NORTH -> entity.mcu.frontOutput;
            default -> 0;
        };
    }

    @Override
    protected int getDirectSignal(@NotNull BlockState blockState, @NotNull BlockGetter blockGetter, @NotNull BlockPos blockPos, @NotNull Direction direction) {
        return getSignal(blockState, blockGetter, blockPos, direction);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, @NotNull BlockState state,
            @NotNull BlockEntityType<T> type) {
        if (world.isClientSide())
            return null;

        return CNnanoBlock.createTickerHelper(type, BlockEntities.CN_NANO, CNnanoBlockEntity::serverTick);
    }


    @Override
    @NotNull
    public InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, Player player,
            @NotNull BlockHitResult hit) {
        if (!world.isClientSide()) {
            MenuProvider screenHandlerFactory = state.getMenuProvider(world, pos);

            if (screenHandlerFactory != null)
                player.openMenu(screenHandlerFactory);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    @NotNull
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);
    }
    
    public static Direction getGlobalDirection(Direction facing, Direction direction) {
        return switch (facing) {
            case EAST -> direction.getClockWise();
            case SOUTH -> direction.getOpposite();
            case WEST -> direction.getCounterClockWise();
            default -> direction;
        };
    }
    
    public static Direction getLocalDirection(Direction facing, Direction direction) {
        return switch (facing) {
            case WEST -> direction.getCounterClockWise();
            case NORTH -> direction.getOpposite();
            case EAST -> direction.getClockWise();
            default -> direction;
        };
    }
}
