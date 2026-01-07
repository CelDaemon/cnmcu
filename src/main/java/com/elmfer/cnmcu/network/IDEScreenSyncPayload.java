package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;
import com.elmfer.cnmcu.mcu.NanoMCU;
import com.elmfer.cnmcu.ui.IDEScreen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record IDEScreenSyncPayload(
        boolean isPowered,
        boolean isClockPaused,
        CPUStatus cpuStatus,
        BusStatus busStatus,
        byte[] zeroPage) implements CustomPacketPayload {
    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("ide_screen_sync");
    public static final CustomPacketPayload.Type<IDEScreenSyncPayload> ID = new CustomPacketPayload.Type<>(RAW_ID);
    public static final StreamCodec<FriendlyByteBuf, IDEScreenSyncPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, IDEScreenSyncPayload::isPowered,
            ByteBufCodecs.BOOL, IDEScreenSyncPayload::isClockPaused,
            CPUStatus.PACKET_CODEC, IDEScreenSyncPayload::cpuStatus,
            BusStatus.PACKET_CODEC, IDEScreenSyncPayload::busStatus,
            ByteBufCodecs.byteArray(256), IDEScreenSyncPayload::zeroPage, // More than 256??
            IDEScreenSyncPayload::new
    );

    public static IDEScreenSyncPayload create(CNnanoBlockEntity blockEntity) {
        var mcu = blockEntity.mcu;
        var dataBuffer = mcu.getRAM().getData();
        var data = new byte[256];
        dataBuffer.get(data);
        return new IDEScreenSyncPayload(
                mcu.isPowered(),
                mcu.isClockPaused(),
                CPUStatus.create(mcu),
                BusStatus.create(mcu),
                data
        );
    }
    
    public static void receive(IDEScreenSyncPayload payload, ClientPlayNetworking.Context context) {
        @SuppressWarnings("resource") var client = context.client();
        if(!(client.screen instanceof IDEScreen screen))
            return;

        CPUStatus cpuStatus = payload.cpuStatus();
        BusStatus busStatus = payload.busStatus();

        screen.isPowered = payload.isPowered();
        screen.isClockPaused = payload.isClockPaused();

        screen.cpuStatus = cpuStatus;
        screen.busStatus = busStatus;
        screen.zeroPage.clear();
        screen.zeroPage.put(payload.zeroPage());
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public record CPUStatus(
            int a,
            int x,
            int y,
            int pc,
            int sp,
            int flags,
            long cycles) {
        public static final StreamCodec<FriendlyByteBuf, CPUStatus> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, CPUStatus::a,
                ByteBufCodecs.INT, CPUStatus::x,
                ByteBufCodecs.INT, CPUStatus::y,
                ByteBufCodecs.INT, CPUStatus::pc,
                ByteBufCodecs.INT, CPUStatus::sp,
                ByteBufCodecs.INT, CPUStatus::flags,
                ByteBufCodecs.LONG, CPUStatus::cycles,
                CPUStatus::new
        );

        public static CPUStatus create(NanoMCU mcu) {
            var cpu = mcu.getCPU();
            return new CPUStatus(
                    cpu.getA(),
                    cpu.getX(),
                    cpu.getY(),
                    cpu.getPC(),
                    cpu.getS(),
                    cpu.getP(),
                    mcu.numCycles()
            );
        }
    }
    
    public record BusStatus(int address, int data, boolean rw) {
        public static final StreamCodec<FriendlyByteBuf, BusStatus> PACKET_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, BusStatus::address,
                ByteBufCodecs.INT, BusStatus::data,
                ByteBufCodecs.BOOL, BusStatus::rw,
                BusStatus::new
        );

        public static BusStatus create(NanoMCU mcu) {
            return new BusStatus(
                    mcu.busAddress(),
                    mcu.busData(),
                    mcu.busRW()
            );
        }
    }
}
