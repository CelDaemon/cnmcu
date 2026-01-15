package com.elmfer.cnmcu.blockentities;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blocks.CNnanoBlock;
import com.elmfer.cnmcu.DataComponents;
import com.elmfer.cnmcu.mcu.NanoMCU;
import com.elmfer.cnmcu.ui.menu.IDEMenu;
import com.elmfer.cnmcu.util.DirectionUtil;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

public class CNnanoBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<IDEMenu.OpenData> {
    
    public final NanoMCU mcu = new NanoMCU();
    private String code = "";

    public CNnanoBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CN_NANO, pos, state);

        var mcu = this.mcu;

        CodeNodeMicrocontrollers.CLEANER.register(this, mcu::deleteNativeObject);
    }

    public static void serverTick(@NotNull Level ignoredLevel, @NotNull BlockPos ignoredPos, @NotNull BlockState ignoredState, @NotNull CNnanoBlockEntity blockEntity) {

        var mcu = blockEntity.mcu;

        if (!mcu.isPowered() || mcu.isClockPaused())
            return;

        blockEntity.loadInputs();
        mcu.tick();
        blockEntity.writeOutputs();

        blockEntity.setChanged();
    }

    public void cycle() {

        if(!mcu.isPowered() || !mcu.isClockPaused())
            return;

        loadInputs();
        mcu.cycle();
        writeOutputs();

        setChanged();
    }

    private void loadInputs() {
        assert mcu != null;
        assert level != null;

        var state = getBlockState();
        var pos = getBlockPos();

        var front = state.getValue(CNnanoBlock.FACING).getOpposite();
        var right = front.getClockWise();
        var back = front.getOpposite();
        var left = front.getCounterClockWise();

        mcu.frontInput = level.getSignal(pos.relative(front), front);
        mcu.rightInput = level.getSignal(pos.relative(right), right);
        mcu.backInput = level.getSignal(pos.relative(back), back);
        mcu.leftInput = level.getSignal(pos.relative(left), left);
    }

    private void writeOutputs() {
        assert mcu != null;
        assert level != null;

        var state = getBlockState();
        var pos = getBlockPos();
        var front = state.getValue(CNnanoBlock.FACING);

        var block = state.getBlock();

        for (var localDirection : Direction.Plane.HORIZONTAL) {
            if (!mcu.outputHasChanged(localDirection))
                continue;
            var globalDirection = DirectionUtil.rotate(front, localDirection);
            var targetSide = globalDirection.getOpposite();
            var neighborPosition = pos.relative(targetSide);
            var orientation = ExperimentalRedstoneUtils.initialOrientation(level, targetSide, null);
            level.neighborChanged(neighborPosition, state.getBlock(), orientation);
            level.updateNeighborsAtExceptFromFacing(neighborPosition, block, globalDirection, orientation);
        }
    }

    public void setPowered(boolean powered) {
        mcu.setPowered(powered);
        writeOutputs();
        setChanged();
    }

    public boolean isPowered() {
        return mcu.isPowered();
    }

    public void setClockPause(boolean paused) {

        mcu.setClockPause(paused);
        setChanged();
    }

    public boolean isClockPaused() {
        return mcu.isClockPaused();
    }

    public void reset() {
        mcu.reset();
        writeOutputs();
        setChanged();
    }

    public void setCode(String code) {
        this.code = code;
        setChanged();
    }
    public String getCode() {
        return code;
    }

    @Override
    public void saveAdditional(@NotNull ValueOutput view) {
        super.saveAdditional(view);

        view.store("mcu", NanoMCU.State.CODEC, mcu.getState());
        view.putString("code", code);
    }

    @Override
    public void loadAdditional(@NotNull ValueInput view) {
        super.loadAdditional(view);

        if(view.contains("mcu"))
            mcu.setState(view.read("mcu", NanoMCU.State.CODEC).orElseThrow());
        code = view.getStringOr("code", "");
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CODE, this.code);
        builder.set(DataComponents.MCU, this.mcu.getState());
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentGetter dataComponentGetter) {
        super.applyImplicitComponents(dataComponentGetter);

        this.code = dataComponentGetter.getOrDefault(DataComponents.CODE, "");
        var mcuState = dataComponentGetter.get(DataComponents.MCU);
        if(mcuState != null)
            mcu.setState(mcuState);
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.literal("Code Node Nano");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        assert level != null;
        return new IDEMenu(containerId, ContainerLevelAccess.create(level, getBlockPos()), this);
    }

    @Override
    public IDEMenu.OpenData getScreenOpeningData(@NotNull ServerPlayer player) {
        return new IDEMenu.OpenData(
                code
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        valueOutput.discard("code");
        valueOutput.discard("mcu");
    }

}
