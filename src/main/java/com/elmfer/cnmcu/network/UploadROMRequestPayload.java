package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public record UploadROMRequestPayload(
        int transactionId,
        UUID mcuId,
        byte[] rom
) implements CustomPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadROMRequestPayload.class);
    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("upload_rom_request");
    public static final CustomPayload.Id<UploadROMRequestPayload> ID = new CustomPayload.Id<>(RAW_ID);

    public static final PacketCodec<PacketByteBuf, UploadROMRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.SYNC_ID, UploadROMRequestPayload::transactionId,
            Uuids.PACKET_CODEC, UploadROMRequestPayload::mcuId,
            PacketCodecs.BYTE_ARRAY, UploadROMRequestPayload::rom,
            UploadROMRequestPayload::new
    );

    private static final ConcurrentHashMap<Integer, CompletableFuture<UploadROMResponsePayload>> TRANSACTIONS = new ConcurrentHashMap<>();
    private static int nextTransactionId = 0;

    @Override
    public Id<? extends CustomPayload> getId() {
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

        var mcu = CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).getEntity().mcu;

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
            mcu.setPowered(false);
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
        LOGGER.info("Received response for transaction: {}", responsePayload.transactionId());
        var transaction = TRANSACTIONS.remove(responsePayload.transactionId());
        if(transaction == null)
            return;
        transaction.complete(responsePayload);
    }
}
