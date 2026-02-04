package com.elmfer.cnmcu.client.network;

import com.elmfer.cnmcu.network.IDEScreenSyncPayload;
import com.elmfer.cnmcu.network.UploadROMResponsePayload;
import com.elmfer.cnmcu.client.screen.IDEScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(IDEScreenSyncPayload.ID, IDEScreen::receiveSync);
        ClientPlayNetworking.registerGlobalReceiver(UploadROMResponsePayload.ID, UploadROMTransaction::receiveResponse);
    }
}
