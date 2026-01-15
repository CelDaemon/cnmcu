package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record UploadROMResponsePayload(
        int transactionId,
        int bytesUploaded,
        String message) implements CustomPacketPayload {

    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id(
            "upload_rom_response");
    public static final CustomPacketPayload.Type<UploadROMResponsePayload> ID = new CustomPacketPayload.Type<>(RAW_ID);
    public static final StreamCodec<FriendlyByteBuf, UploadROMResponsePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.CONTAINER_ID, UploadROMResponsePayload::transactionId,
            ByteBufCodecs.INT, UploadROMResponsePayload::bytesUploaded,
            ByteBufCodecs.STRING_UTF8, UploadROMResponsePayload::message,
            UploadROMResponsePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void receive(UploadROMResponsePayload payload, ClientPlayNetworking.Context ignoredContext) {
        UploadROMRequestPayload.notifyResponse(payload);
    }
}
