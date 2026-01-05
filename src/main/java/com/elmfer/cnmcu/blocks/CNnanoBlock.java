package com.elmfer.cnmcu.blocks;

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
    public MapCodec<CNnanoBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CNnanoBlockEntity(pos, state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
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
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof CNnanoBlockEntity))
            return 0;

        CNnanoBlockEntity entity = (CNnanoBlockEntity) blockEntity;

        if (entity.mcu == null || !entity.mcu.isPowered())
            return 0;
        
        Direction blockDir = state.getValue(FACING);
        Direction localDir = getLocalDirection(blockDir, direction);

        switch (localDir) {
        case EAST:
            return entity.mcu.rightOutput;
        case SOUTH:
            return entity.mcu.backOutput;
        case WEST:
            return entity.mcu.leftOutput;
        case NORTH:
            return entity.mcu.frontOutput;
        default:
            return 0;
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
            BlockEntityType<T> type) {
        if (world.isClientSide())
            return null;

        return CNnanoBlock.createTickerHelper(type, BlockEntities.CN_NANO, CNnanoBlockEntity::tick);
    }


    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!world.isClientSide()) {
            MenuProvider screenHandlerFactory = state.getMenuProvider(world, pos);

            if (screenHandlerFactory != null)
                player.openMenu(screenHandlerFactory);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
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
