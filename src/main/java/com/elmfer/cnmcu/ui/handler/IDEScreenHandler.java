package com.elmfer.cnmcu.ui.handler;

import java.util.UUID;

import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Uuids;

public class IDEScreenHandler extends ScreenHandler {

    private UUID mcuID;
    private String code;
    
    public IDEScreenHandler(int syncId, PlayerInventory playerInventory, OpenData data) {
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
    public void onClosed(PlayerEntity player) {
        if (!player.getEntityWorld().isClient() && CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuID))
            CNnanoBlockEntity.SCREEN_UPDATES.get(mcuID).removeListener(player.getUuid());
    }
   
    @Override
    public ItemStack quickMove(PlayerEntity var1, int var2) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity var1) {
        // TODO Auto-generated method stub
        return true;
    }

    public record OpenData(
            UUID mcuId,
            String code
    ) {
       public static final PacketCodec<ByteBuf, OpenData> PACKET_CODEC = PacketCodec.tuple(
               Uuids.PACKET_CODEC, OpenData::mcuId,
               PacketCodecs.STRING, OpenData::code,
               OpenData::new
       );
    }
}
