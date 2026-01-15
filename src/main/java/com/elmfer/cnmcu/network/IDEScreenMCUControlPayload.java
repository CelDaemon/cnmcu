package com.elmfer.cnmcu.network;

import java.util.function.IntFunction;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;

import com.elmfer.cnmcu.ui.menu.IDEMenu;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
import org.jetbrains.annotations.NotNull;

import static com.elmfer.cnmcu.CodeNodeMicrocontrollers.LOGGER;

public record IDEScreenMCUControlPayload(
        int containerId,
        Control control) implements CustomPacketPayload {
    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("ide_screen_mcu_control");
    public static final Type<IDEScreenMCUControlPayload> ID = new Type<>(RAW_ID);
    public static final StreamCodec<FriendlyByteBuf, IDEScreenMCUControlPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.CONTAINER_ID, IDEScreenMCUControlPayload::containerId,
            Control.STREAM_CODEC, IDEScreenMCUControlPayload::control,
            IDEScreenMCUControlPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void receive(IDEScreenMCUControlPayload payload, ServerPlayNetworking.Context ignoredContext) {
        var menu = ignoredContext.player().containerMenu;

        if(menu.containerId != payload.containerId()) {
            LOGGER.debug("Ignoring save packet with mismatched container id");
            return;
        }

        if(!(menu instanceof IDEMenu ideMenu)) {
            LOGGER.debug("Sent save payload with invalid menu type");
            return;
        }
        var control = payload.control();

        var blockEntity = ideMenu.blockEntity;

        assert blockEntity != null;
        
        switch (control) {
        case POWER_ON:
            blockEntity.setPowered(true);
            break;
        case POWER_OFF:
            blockEntity.setPowered(false);
            break;
        case RESET:
            blockEntity.reset();
            break;
        case PAUSE_CLOCK:
            if (blockEntity.isPowered())
                blockEntity.setClockPause(true);
            break;
        case RESUME_CLOCK:
            if (blockEntity.isPowered())
                blockEntity.setClockPause(false);
            break;
        case CYCLE:
            if (blockEntity.isClockPaused())
                blockEntity.cycle();
            break;
        }
    }
    
    public enum Control {
        POWER_ON(0),
        POWER_OFF(1),
        RESET(2),
        PAUSE_CLOCK(3),
        RESUME_CLOCK(4),
        CYCLE(5);
        public static final IntFunction<Control> BY_ID = ByIdMap.continuous(Control::getId, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, Control> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Control::getId);
        private final int id;
        Control(int id) {
            this.id = id;
        }
        public int getId() {
            return id;
        }
    }
}
