package com.elmfer.cnmcu.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Networking {

    public static void register() {
        var s2c = PayloadTypeRegistry.playS2C();
        s2c.register(IDEScreenSyncPayload.ID, IDEScreenSyncPayload.CODEC);
        s2c.register(UploadROMResponsePayload.ID, UploadROMResponsePayload.CODEC);

        var c2s = PayloadTypeRegistry.playC2S();
        c2s.register(IDEScreenSaveCodePayload.TYPE, IDEScreenSaveCodePayload.CODEC);
        c2s.register(UploadROMRequestPayload.ID, UploadROMRequestPayload.CODEC);
        c2s.register(IDEScreenMCUControlPayload.ID, IDEScreenMCUControlPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(IDEScreenSaveCodePayload.TYPE, IDEScreenSaveCodePayload::receive);
        ServerPlayNetworking.registerGlobalReceiver(UploadROMRequestPayload.ID, UploadROMRequestPayload::receive);
        ServerPlayNetworking.registerGlobalReceiver(IDEScreenMCUControlPayload.ID, IDEScreenMCUControlPayload::receive);
    }
}
