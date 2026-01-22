package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CNMCU;
import com.elmfer.cnmcu.ui.menu.IDEMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.elmfer.cnmcu.CNMCU.LOGGER;

public record UploadROMRequestPayload(
        int transactionId,
        int containerId,
        byte[] rom
) implements CustomPacketPayload {
    public static final Identifier RAW_ID = CNMCU.id("upload_rom_request");
    public static final CustomPacketPayload.Type<UploadROMRequestPayload> ID = new CustomPacketPayload.Type<>(RAW_ID);

    public static final StreamCodec<FriendlyByteBuf, UploadROMRequestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UploadROMRequestPayload::transactionId,
            ByteBufCodecs.CONTAINER_ID, UploadROMRequestPayload::containerId,
            ByteBufCodecs.BYTE_ARRAY, UploadROMRequestPayload::rom,
            UploadROMRequestPayload::new
    );

    private static final ConcurrentHashMap<Integer, CompletableFuture<UploadROMResponsePayload>> TRANSACTIONS = new ConcurrentHashMap<>();
    private static int nextTransactionId = 0;

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void receive(UploadROMRequestPayload payload, ServerPlayNetworking.Context context) {
        var player = context.player();
        var transactionId = payload.transactionId();

        var menu = context.player().containerMenu;

        if(menu.containerId != payload.containerId()) {
            LOGGER.debug("Ignoring save packet with mismatched container id");
            return;
        }

        if(!(menu instanceof IDEMenu ideMenu)) {
            LOGGER.debug("Sent save payload with invalid menu type");
            return;
        }

        var blockEntity = ideMenu.blockEntity;

        assert blockEntity != null;

        var rom = payload.rom();

        var mcu = blockEntity.mcu;

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
            blockEntity.setPowered(false);
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

    public static CompletableFuture<UploadROMResponsePayload> send(int containerId, byte[] rom) {
        var transactionId = nextTransactionId++;
        var future = new CompletableFuture<UploadROMResponsePayload>();
        TRANSACTIONS.put(transactionId, future);
        ClientPlayNetworking.send(new UploadROMRequestPayload(transactionId, containerId, rom));
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
