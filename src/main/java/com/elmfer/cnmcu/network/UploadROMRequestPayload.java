package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.LOGGER;

public record UploadROMRequestPayload(
        int transactionId,
        UUID mcuId,
        byte[] rom
) implements CustomPacketPayload {
    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("upload_rom_request");
    public static final CustomPacketPayload.Type<UploadROMRequestPayload> ID = new CustomPacketPayload.Type<>(RAW_ID);

    public static final StreamCodec<FriendlyByteBuf, UploadROMRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.CONTAINER_ID, UploadROMRequestPayload::transactionId,
            UUIDUtil.STREAM_CODEC, UploadROMRequestPayload::mcuId,
            ByteBufCodecs.BYTE_ARRAY, UploadROMRequestPayload::rom,
            UploadROMRequestPayload::new
    );

    private static final ConcurrentHashMap<Integer, CompletableFuture<UploadROMResponsePayload>> TRANSACTIONS = new ConcurrentHashMap<>();
    private static int nextTransactionId = 0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void receive(UploadROMRequestPayload payload, ServerPlayNetworking.Context context) {
        var player = context.player();
        var mcuId = payload.mcuId();
        var transactionId = payload.transactionId();

        if(!CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuId)) {
            ServerPlayNetworking.send(player, new UploadROMResponsePayload(
                    transactionId,
                    0,
                    "MCU not found."
            ));
            return;
        }

        var rom = payload.rom();

        var entity = CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).getEntity();

        var mcu = entity.mcu;

        var mcuRomSize = mcu.getROM().getSize();
        if(mcuRomSize != rom.length) {
            ServerPlayNetworking.send(player, new UploadROMResponsePayload(
                    transactionId,
                    0,
                    String.format("Binary size mismatch. Expected: %d, Got: %d", mcuRomSize, rom.length)
            ));
            return;
        }

        @SuppressWarnings("resource") var server = context.server();
        server.execute(() -> {
            entity.setPowered(false);
            var data = mcu.getROM().getData();
            data.clear();
            data.put(rom);
        });

        ServerPlayNetworking.send(player, new UploadROMResponsePayload(
                transactionId,
                rom.length,
                String.format("Uploaded %d bytes.", rom.length)
        ));
    }

    public static Future<UploadROMResponsePayload> send(UUID mcuId, byte[] rom) {
        var transactionId = nextTransactionId++;
        var future = new CompletableFuture<UploadROMResponsePayload>();
        TRANSACTIONS.put(transactionId, future);
        ClientPlayNetworking.send(new UploadROMRequestPayload(transactionId, mcuId, rom));
        return future;
    }

    public static void notifyResponse(UploadROMResponsePayload responsePayload) {
        LOGGER.debug("Received response for transaction: {}", responsePayload.transactionId());
        var transaction = TRANSACTIONS.remove(responsePayload.transactionId());
        if(transaction == null)
            return;
        transaction.complete(responsePayload);
    }
}
