package com.elmfer.cnmcu.blockentities;

import java.util.*;

import com.elmfer.cnmcu.blocks.CNnanoBlock;
import com.elmfer.cnmcu.mcu.NanoMCU;
import com.elmfer.cnmcu.network.IDEScreenSyncPayload;
import com.elmfer.cnmcu.ui.handler.IDEMenu;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    public static final Map<UUID, ScreenUpdates> SCREEN_UPDATES = new HashMap<>();
    
    public NanoMCU mcu;
    private UUID uuid;
    private boolean hasInit = false;

    private String code = "";
    
    public CNnanoBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntities.CN_NANO, pos, state);
    }

    public static void serverTick(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull CNnanoBlockEntity blockEntity) {
        if(!blockEntity.hasInit)
            blockEntity.init();

        SCREEN_UPDATES.get(blockEntity.uuid).handleScreenListeners();

        var mcu = blockEntity.mcu;

        if(!mcu.isPowered())
            return;
        
        var front = state.getValue(CNnanoBlock.FACING);
        var right = front.getClockWise();
        var back = front.getOpposite();
        var left = front.getCounterClockWise();
        
        mcu.frontInput = level.getSignal(pos.relative(front), front);
        mcu.rightInput = level.getSignal(pos.relative(right), right);
        mcu.backInput = level.getSignal(pos.relative(back), back);
        mcu.leftInput = level.getSignal(pos.relative(left), left);

        mcu.tick();
        blockEntity.setChanged();

        var block = state.getBlock();
        
        for (var localDirection : Direction.Plane.HORIZONTAL) {
            if(!blockEntity.mcu.outputHasChanged(localDirection))
                continue;
            var globalDirection = CNnanoBlock.getGlobalDirection(front, localDirection);
            var neighborPosition = pos.relative(globalDirection);
            var orientation = ExperimentalRedstoneUtils.initialOrientation(level, globalDirection, null);
            level.neighborChanged(neighborPosition, state.getBlock(), orientation);
            level.updateNeighborsAtExceptFromFacing(neighborPosition, block, globalDirection, orientation);
        }
    }

    public void setPowered(boolean powered) {
        mcu.setPowered(powered);
        setChanged();
        if(level != null)
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    public void reset() {
        mcu.reset();
        setChanged();
    }
    
    protected void init() {
        if(hasInit)
            return;
        
        uuid = UUID.randomUUID();
        SCREEN_UPDATES.put(uuid, new ScreenUpdates(this));
        
        if (mcu == null)
            mcu = new NanoMCU();
        
        hasInit = true;
    }
    
    public UUID getUUID() {
        return uuid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        setChanged();
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        
        if (!hasInit)
            init();
        
        if(!view.contains("code"))
            return;
        
        mcu.setState(view.read("mcu", NanoMCU.State.CODEC).orElseThrow());
        code = view.getStringOr("code", "");

    }

    @Override
    public void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        
        if (!hasInit)
            init();

        view.store("mcu", NanoMCU.State.CODEC, mcu.getState());
        view.putString("code", code);
    }

   @Override
   public void setRemoved() {
       super.setRemoved();
       
       if(mcu != null) {
           mcu.deleteNativeObject();
           mcu = null;
       }
       
       SCREEN_UPDATES.remove(uuid);
   }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("Code Node Nano");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        SCREEN_UPDATES.get(uuid).addListener((ServerPlayer) player);
        assert level != null;
        return new IDEMenu(containerId, uuid, ContainerLevelAccess.create(level, getBlockPos()));
    }

    @Override
    public IDEMenu.OpenData getScreenOpeningData(ServerPlayer player) {
        return new IDEMenu.OpenData(
                uuid,
                code
        );
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
