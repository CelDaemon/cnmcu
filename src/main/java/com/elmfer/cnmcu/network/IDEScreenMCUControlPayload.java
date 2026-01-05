package com.elmfer.cnmcu.network;

import java.util.UUID;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IDEScreenMCUControlPayload(
        UUID mcuId,
        Control control) implements CustomPacketPayload {
    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("ide_screen_mcu_control");
    public static final Type<IDEScreenMCUControlPayload> ID = new Type<>(RAW_ID);
    public static final StreamCodec<FriendlyByteBuf, IDEScreenMCUControlPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, IDEScreenMCUControlPayload::mcuId,
            Control.PACKET_CODEC, IDEScreenMCUControlPayload::control,
            IDEScreenMCUControlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void receive(IDEScreenMCUControlPayload payload, ServerPlayNetworking.Context ignoredContext) {
        var mcuId = payload.mcuId();
        
        if (!CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuId))
            return;

        var control = payload.control();
        CNnanoBlockEntity entity = CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).getEntity();
        
        switch (control) {
        case POWER_ON:
            entity.setPowered(true);
            break;
        case POWER_OFF:
            entity.setPowered(false);
            break;
        case RESET:
            entity.reset();
            break;
        case PAUSE_CLOCK:
            if (entity.mcu.isPowered())
                entity.mcu.setClockPause(true);
            break;
        case RESUME_CLOCK:
            if (entity.mcu.isPowered())
                entity.mcu.setClockPause(false);
            break;
        case CYCLE:
            if (entity.mcu.isClockPaused())
                entity.mcu.cycle();
            break;
        }
    }
    
    public enum Control {
        POWER_ON(0),
        POWER_OFF(1),
        RESET(2),
        PAUSE_CLOCK(3),
        RESUME_CLOCK(4),
        CYCLE(5);

        public static final StreamCodec<ByteBuf, Control> PACKET_CODEC = ByteBufCodecs.idMapper(Control::fromId, Control::getId);
        private final int id;
        
        Control(int id) {
            this.id = id;
        }
        public static Control fromId(int id) {
            return switch(id) {
                case 0 -> POWER_ON;
                case 1 -> POWER_OFF;
                case 2 -> RESET;
                case 3 -> PAUSE_CLOCK;
                case 4 -> RESUME_CLOCK;
                case 5 -> CYCLE;
                default -> throw new IllegalStateException("Unexpected value: " + id);
            };
        }
        
        public int getId() {
            return id;
        }
    }
}
