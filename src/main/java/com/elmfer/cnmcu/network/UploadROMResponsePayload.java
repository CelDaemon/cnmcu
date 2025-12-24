package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UploadROMResponsePayload(
        int transactionId,
        int bytesUploaded,
        String message) implements CustomPayload {

    public static final Identifier RAW_ID = Identifier.of(CodeNodeMicrocontrollers.MOD_ID, "upload_rom_response");
    public static final CustomPayload.Id<UploadROMResponsePayload> ID = new CustomPayload.Id<>(RAW_ID);
    public static final PacketCodec<RegistryByteBuf, UploadROMResponsePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.SYNC_ID, UploadROMResponsePayload::transactionId,
            PacketCodecs.INTEGER, UploadROMResponsePayload::bytesUploaded,
            PacketCodecs.STRING, UploadROMResponsePayload::message,
            UploadROMResponsePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void receive(UploadROMResponsePayload payload, ClientPlayNetworking.Context context) {
        // DO NOTHING.
    }
}
