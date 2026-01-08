package com.elmfer.cnmcu.blockentities;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blocks.CNnanoBlock;
import com.elmfer.cnmcu.DataComponents;
import com.elmfer.cnmcu.mcu.NanoMCU;
import com.elmfer.cnmcu.network.IDEScreenSyncPayload;
import com.elmfer.cnmcu.ui.handler.IDEMenu;
import com.elmfer.cnmcu.util.DirectionUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CNnanoBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<IDEMenu.OpenData> {

    public static final Map<UUID, ScreenUpdates> SCREEN_UPDATES = new HashMap<>();
    
    public final NanoMCU mcu = new NanoMCU();
    private final UUID uuid = UUID.randomUUID();
    private String code = "";

    public CNnanoBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CN_NANO, pos, state);

        var mcu = this.mcu;
        var uuid = this.uuid;
        SCREEN_UPDATES.put(uuid, new ScreenUpdates(this));

        CodeNodeMicrocontrollers.CLEANER.register(this, () -> {
            mcu.deleteNativeObject();
            SCREEN_UPDATES.remove(uuid);
        });
    }

    public static void serverTick(@NotNull Level ignoredLevel, @NotNull BlockPos ignoredPos, @NotNull BlockState ignoredState, @NotNull CNnanoBlockEntity blockEntity) {

        var mcu = blockEntity.mcu;

        SCREEN_UPDATES.get(blockEntity.uuid).handleScreenListeners();

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
        SCREEN_UPDATES.get(uuid).addListener((ServerPlayer) player);
        assert level != null;
        return new IDEMenu(containerId, uuid, ContainerLevelAccess.create(level, getBlockPos()));
    }

    @Override
    public IDEMenu.OpenData getScreenOpeningData(@NotNull  ServerPlayer player) {
        return new IDEMenu.OpenData(
                uuid,
                code
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public void removeComponentsFromTag(ValueOutput valueOutput) {
        valueOutput.discard("code");
        valueOutput.discard("mcu");
    }

    public static class Listener {
        ServerPlayer player;
        int ticksSinceLastHeartbeat = 0;
        boolean shouldRemove = false;
        
        public Listener(ServerPlayer player) {
            this.player = player;
        }
        
        public void update(IDEScreenSyncPayload syncPacket) {
            
            if (player.hasDisconnected()) {
                shouldRemove = true;
                return;
            }
            ServerPlayNetworking.send(player, syncPacket);
            
            if (ticksSinceLastHeartbeat >= ScreenUpdates.NEXT_HEARTBEAT_EXPECTATION) {
                shouldRemove = true;
                return;
            }
            
            ticksSinceLastHeartbeat++;
        }
    }
    
    public static class ScreenUpdates {
        static final int NEXT_HEARTBEAT_EXPECTATION = 100;
        
        Map<UUID, Listener> listeners = new HashMap<>();
        CNnanoBlockEntity entity;
        
        public ScreenUpdates(CNnanoBlockEntity entity) {
            this.entity = entity;
            
        }
        
        public void addListener(ServerPlayer player) {
            if(listeners.containsKey(player.getUUID()))
                return;
            
            listeners.put(player.getUUID(), new Listener(player));
        }
        
        public void removeListener(UUID playerUuid) {
            listeners.get(playerUuid).shouldRemove = true;
        }
        
        public void heartBeat(UUID playerUuid) {
            if (!listeners.containsKey(playerUuid))
                return;
            
            listeners.get(playerUuid).ticksSinceLastHeartbeat = 0;
        }
        
        public CNnanoBlockEntity getEntity() {
            return entity;
        }
        
        public void handleScreenListeners() {
            var syncPacket = IDEScreenSyncPayload.create(entity);
            
            listeners.entrySet().removeIf(entry -> {
                Listener listener = entry.getValue();
                listener.update(syncPacket);
                
                if (listener.shouldRemove)
                    listener.player.closeContainer();
                
                return listener.shouldRemove;
            });
        }
    }
}
