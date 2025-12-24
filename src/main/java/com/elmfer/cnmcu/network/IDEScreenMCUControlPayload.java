package com.elmfer.cnmcu.network;

import java.util.UUID;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public record IDEScreenMCUControlPayload(
        UUID mcuId,
        Control control) implements CustomPayload {
    public static final Identifier RAW_ID = Identifier.of(CodeNodeMicrocontrollers.MOD_ID, "ide_screen_mcu_control");
    public static final Id<IDEScreenMCUControlPayload> ID = new Id<>(RAW_ID);
    public static final PacketCodec<RegistryByteBuf, IDEScreenMCUControlPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, IDEScreenMCUControlPayload::mcuId,
            Control.PACKET_CODEC, IDEScreenMCUControlPayload::control,
            IDEScreenMCUControlPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void receive(IDEScreenMCUControlPayload payload, ServerPlayNetworking.Context context) {
        var mcuId = payload.mcuId();
        
        if (!CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuId))
            return;

        var control = payload.control();
        CNnanoBlockEntity entity = CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).getEntity();
        
        switch (control) {
        case POWER_ON:
            entity.mcu.setPowered(true);
            break;
        case POWER_OFF:
            entity.mcu.setPowered(false);
            break;
        case RESET:
            entity.mcu.reset();
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

        public static final PacketCodec<ByteBuf, Control> PACKET_CODEC = PacketCodecs.indexed(Control::fromId, Control::getId);
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
