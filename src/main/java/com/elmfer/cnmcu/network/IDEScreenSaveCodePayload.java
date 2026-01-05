package com.elmfer.cnmcu.network;

import java.util.UUID;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;

import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IDEScreenSaveCodePayload(
        UUID mcuId,
        String code) implements CustomPacketPayload {
    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("ide_screen_save_code");
    public static final CustomPacketPayload.Type<IDEScreenSaveCodePayload> ID = new CustomPacketPayload.Type<>(RAW_ID);
    public static final StreamCodec<FriendlyByteBuf, IDEScreenSaveCodePayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, IDEScreenSaveCodePayload::mcuId,
            ByteBufCodecs.STRING_UTF8, IDEScreenSaveCodePayload::code,
            IDEScreenSaveCodePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void receive(IDEScreenSaveCodePayload payload, ServerPlayNetworking.Context ignoredContext) {
        UUID mcuId = payload.mcuId();

        String codeStr = payload.code();


        if (CNnanoBlockEntity.SCREEN_UPDATES.containsKey(mcuId)) {
            CNnanoBlockEntity entity = CNnanoBlockEntity.SCREEN_UPDATES.get(mcuId).getEntity();
            entity.setCode(codeStr);
        }

    }
}
