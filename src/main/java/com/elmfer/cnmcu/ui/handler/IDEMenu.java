package com.elmfer.cnmcu.ui.handler;

import java.util.UUID;

import com.elmfer.cnmcu.blocks.Blocks;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

public class IDEMenu extends AbstractContainerMenu {

    private final UUID mcuID;
    private final String code;

    private final ContainerLevelAccess containerAccess;
    
    public IDEMenu(int containerId, Inventory ignoredPlayerInventory, OpenData data) {
        super(Menus.IDE_MENU, containerId);

        mcuID = data.mcuId;
        code = data.code;
        containerAccess = ContainerLevelAccess.NULL;
    }
    public IDEMenu(int containerId, UUID mcuID, ContainerLevelAccess containerAccess) {
        super(Menus.IDE_MENU, containerId);
        
        this.mcuID = mcuID;
        this.code = "";
        this.containerAccess = containerAccess;
    }

    public UUID getMcuID() {
        return mcuID;
    }

    public String getCode() {
        return code;
    }
    
    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide() && CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuID))
            CNnanoBlockEntity.SCREEN_UPDATES.get(mcuID).removeListener(player.getUUID());
    }
   
    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(containerAccess, player, Blocks.CN_NANO_BLOCK);
    }

    public record OpenData(
            UUID mcuId,
            String code
    ) {
       public static final StreamCodec<ByteBuf, OpenData> STREAM_CODEC = StreamCodec.composite(
               UUIDUtil.STREAM_CODEC, OpenData::mcuId,
               ByteBufCodecs.STRING_UTF8, OpenData::code,
               OpenData::new
       );
    }
}
