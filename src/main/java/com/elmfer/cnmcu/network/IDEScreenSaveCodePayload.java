package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CNMCU;

import com.elmfer.cnmcu.ui.menu.IDEMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.elmfer.cnmcu.CNMCU.LOGGER;

public record IDEScreenSaveCodePayload(
        int containerId,
        String code) implements CustomPacketPayload {
    public static final Identifier RAW_ID = CNMCU.id("ide_screen_save_code");
    public static final CustomPacketPayload.Type<IDEScreenSaveCodePayload> TYPE = new CustomPacketPayload.Type<>(RAW_ID);
    public static final StreamCodec<FriendlyByteBuf, IDEScreenSaveCodePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.CONTAINER_ID, IDEScreenSaveCodePayload::containerId,
            ByteBufCodecs.STRING_UTF8, IDEScreenSaveCodePayload::code,
            IDEScreenSaveCodePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void receive(IDEScreenSaveCodePayload payload, ServerPlayNetworking.Context ignoredContext) {
        String codeStr = payload.code();

        var menu = ignoredContext.player().containerMenu;

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

        blockEntity.setCode(codeStr);
    }
}
