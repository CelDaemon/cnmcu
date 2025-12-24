package com.elmfer.cnmcu.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Packets {
    public static void registerPackets() {
        var s2c = PayloadTypeRegistry.playS2C();
        s2c.register(IDEScreenSyncPayload.ID, IDEScreenSyncPayload.CODEC);
        s2c.register(UploadROMResponsePayload.ID, UploadROMResponsePayload.CODEC);

        var c2s = PayloadTypeRegistry.playC2S();
        c2s.register(IDEScreenSaveCodePayload.ID, IDEScreenSaveCodePayload.CODEC);
        c2s.register(UploadROMRequestPayload.ID, UploadROMRequestPayload.CODEC);
        c2s.register(IDEScreenHeartbeatPayload.ID, IDEScreenHeartbeatPayload.CODEC);
        c2s.register(IDEScreenMCUControlPayload.ID, IDEScreenMCUControlPayload.CODEC);
    }
    public static void initClientPackets() {
        ClientPlayNetworking.registerGlobalReceiver(IDEScreenSyncPayload.ID, IDEScreenSyncPayload::receive);
        ClientPlayNetworking.registerGlobalReceiver(UploadROMResponsePayload.ID, UploadROMResponsePayload::receive);
    }

    public static void initServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(IDEScreenSaveCodePayload.ID, IDEScreenSaveCodePayload::receive);
        ServerPlayNetworking.registerGlobalReceiver(UploadROMRequestPayload.ID, UploadROMRequestPayload::receive);
        ServerPlayNetworking.registerGlobalReceiver(IDEScreenHeartbeatPayload.ID, IDEScreenHeartbeatPayload::receive);
        ServerPlayNetworking.registerGlobalReceiver(IDEScreenMCUControlPayload.ID, IDEScreenMCUControlPayload::receive);
    }
}
