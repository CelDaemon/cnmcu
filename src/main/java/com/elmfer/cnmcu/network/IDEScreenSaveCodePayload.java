package com.elmfer.cnmcu.network;

import java.util.UUID;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;

import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public record IDEScreenSaveCodePayload(
        UUID mcuId,
        String code) implements CustomPayload {
    public static final Identifier RAW_ID = Identifier.of(CodeNodeMicrocontrollers.MOD_ID, "ide_screen_save_code");
    public static final CustomPayload.Id<IDEScreenSaveCodePayload> ID = new CustomPayload.Id<>(RAW_ID);
    public static final PacketCodec<RegistryByteBuf, IDEScreenSaveCodePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, IDEScreenSaveCodePayload::mcuId,
            PacketCodecs.STRING, IDEScreenSaveCodePayload::code,
            IDEScreenSaveCodePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void receive(IDEScreenSaveCodePayload payload, ServerPlayNetworking.Context context) {
        UUID mcuId = payload.mcuId();

        String codeStr = payload.code();


        if (CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuId)) {
            CNnanoBlockEntity entity = CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).getEntity();
            entity.setCode(codeStr);
            entity.markDirty();
        }

    }
}
