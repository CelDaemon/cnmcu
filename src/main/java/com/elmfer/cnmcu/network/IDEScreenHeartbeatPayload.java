package com.elmfer.cnmcu.network;

import java.util.UUID;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public record IDEScreenHeartbeatPayload(
        UUID mcuId
) implements CustomPayload {

    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("ide_screen_heartbeat");
    public static final CustomPayload.Id<IDEScreenHeartbeatPayload> ID = new CustomPayload.Id<>(RAW_ID);
    public static final PacketCodec<PacketByteBuf, IDEScreenHeartbeatPayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, IDEScreenHeartbeatPayload::mcuId,
            IDEScreenHeartbeatPayload::new
    );

    public static void receive(IDEScreenHeartbeatPayload payload, ServerPlayNetworking.Context context) {
        var mcuId = payload.mcuId();

        @SuppressWarnings("resource") var server = context.server();
        server.execute(() -> {
            if (CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuId))
                CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).heartBeat(context.player().getUuid());
        });
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
