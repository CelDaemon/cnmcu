package com.elmfer.cnmcu.network;

import com.elmfer.cnmcu.CodeNodeMicrocontrollers;
import com.elmfer.cnmcu.blockentities.CNnanoBlockEntity;
import com.elmfer.cnmcu.mcu.NanoMCU;
import com.elmfer.cnmcu.ui.IDEScreen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record IDEScreenSyncPayload(
        boolean isPowered,
        boolean isClockPaused,
        CPUStatus cpuStatus,
        BusStatus busStatus,
        byte[] zeroPage) implements CustomPayload {
    public static final Identifier RAW_ID = CodeNodeMicrocontrollers.id("ide_screen_sync");
    public static final CustomPayload.Id<IDEScreenSyncPayload> ID = new CustomPayload.Id<>(RAW_ID);
    public static final PacketCodec<PacketByteBuf, IDEScreenSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, IDEScreenSyncPayload::isPowered,
            PacketCodecs.BOOLEAN, IDEScreenSyncPayload::isClockPaused,
            CPUStatus.PACKET_CODEC, IDEScreenSyncPayload::cpuStatus,
            BusStatus.PACKET_CODEC, IDEScreenSyncPayload::busStatus,
            PacketCodecs.byteArray(256), IDEScreenSyncPayload::zeroPage, // More than 256??
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
        if(!(client.currentScreen instanceof IDEScreen screen))
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
    public Id<? extends CustomPayload> getId() {
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
        public static final PacketCodec<PacketByteBuf, CPUStatus> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, CPUStatus::a,
                PacketCodecs.INTEGER, CPUStatus::x,
                PacketCodecs.INTEGER, CPUStatus::y,
                PacketCodecs.INTEGER, CPUStatus::pc,
                PacketCodecs.INTEGER, CPUStatus::sp,
                PacketCodecs.INTEGER, CPUStatus::flags,
                PacketCodecs.LONG, CPUStatus::cycles,
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
        public static final PacketCodec<PacketByteBuf, BusStatus> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, BusStatus::address,
                PacketCodecs.INTEGER, BusStatus::data,
                PacketCodecs.BOOLEAN, BusStatus::rw,
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
