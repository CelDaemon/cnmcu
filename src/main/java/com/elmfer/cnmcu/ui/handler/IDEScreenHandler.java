package com.elmfer.cnmcu.ui.handler;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;

import io.netty.buffer.ByteBuf;

public class IDEScreenHandler extends AbstractContainerMenu {

    private UUID mcuID;
    private String code;
    
    public IDEScreenHandler(int syncId, Inventory ignoredPlayerInventory, OpenData data) {
        super(ScreenHandlers.IDE_SCREEN_HANDLER, syncId);

        mcuID = data.mcuId;
        code = data.code;
    }

    public IDEScreenHandler(int syncId, UUID mcuID) {
        super(ScreenHandlers.IDE_SCREEN_HANDLER, syncId);
        
        this.mcuID = mcuID;
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
    public ItemStack quickMoveStack(Player var1, int var2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean stillValid(Player var1) {
        // TODO Auto-generated method stub
        return true;
    }

    public record OpenData(
            UUID mcuId,
            String code
    ) {
       public static final StreamCodec<ByteBuf, OpenData> PACKET_CODEC = StreamCodec.composite(
               UUIDUtil.STREAM_CODEC, OpenData::mcuId,
               ByteBufCodecs.STRING_UTF8, OpenData::code,
               OpenData::new
       );
    }
}
