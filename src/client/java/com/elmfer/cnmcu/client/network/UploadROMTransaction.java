package com.elmfer.cnmcu.client.network;

import com.elmfer.cnmcu.network.UploadROMRequestPayload;
import com.elmfer.cnmcu.network.UploadROMResponsePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.elmfer.cnmcu.CNMCU.LOGGER;

public class UploadROMTransaction {
    private static final ConcurrentHashMap<Integer, CompletableFuture<UploadROMResponsePayload>> TRANSACTIONS = new ConcurrentHashMap<>();
    private static int nextTransactionId = 0;

    public static CompletableFuture<UploadROMResponsePayload> sendRequest(int containerId, byte[] rom) {
        var transactionId = nextTransactionId++;
        var future = new CompletableFuture<UploadROMResponsePayload>();
        TRANSACTIONS.put(transactionId, future);
        ClientPlayNetworking.send(new UploadROMRequestPayload(transactionId, containerId, rom));
        return future;
    }

    public static void receiveResponse(UploadROMResponsePayload payload, ClientPlayNetworking.Context ignoredContext) {
        LOGGER.debug("Received response for transaction: {}", payload.transactionId());
        var transaction = TRANSACTIONS.remove(payload.transactionId());
        if(transaction == null)
            return;
        transaction.complete(payload);
    }
}
