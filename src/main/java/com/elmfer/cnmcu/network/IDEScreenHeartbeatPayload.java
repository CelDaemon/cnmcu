package com.elmfer.cnmcu.network;

import java.util.UUID;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IDEScreenHeartbeatPayload(
        UUID mcuId
) implements CustomPacketPayload {

    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("ide_screen_heartbeat");
    public static final CustomPacketPayload.Type<IDEScreenHeartbeatPayload> ID = new CustomPacketPayload.Type<>(RAW_ID);
    public static final StreamCodec<FriendlyByteBuf, IDEScreenHeartbeatPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, IDEScreenHeartbeatPayload::mcuId,
            IDEScreenHeartbeatPayload::new
    );

    public static void receive(IDEScreenHeartbeatPayload payload, ServerPlayNetworking.Context context) {
        var mcuId = payload.mcuId();

        @SuppressWarnings("resource") var server = context.server();
        server.execute(() -> {
            if (CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuId))
                CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).heartBeat(context.player().getUUID());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
