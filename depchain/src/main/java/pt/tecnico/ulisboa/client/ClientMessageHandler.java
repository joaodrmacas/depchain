package pt.tecnico.ulisboa.client;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.checkerframework.checker.units.qual.m;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class ClientMessageHandler implements MessageHandler {
    private ConcurrentHashMap<Long, ConcurrentHashMap<ClientResp, Integer>> requestResponses;

    public ClientMessageHandler() {
        this.requestResponses = new ConcurrentHashMap<>();
    }

    public void addRequestToWait(Long seqnum) {
        this.requestResponses.put(seqnum, new ConcurrentHashMap<>());
    }

    @Override
    public void onMessage(int senderid, byte[] message) {
        try {
            Logger.LOG("Received message from server: " + senderid);
            ClientResp response = (ClientResp) SerializationUtils.deserializeObject(message);
            Long seqnum = response.getCount();

            ConcurrentHashMap<ClientResp, Integer> numResponses = this.requestResponses.get(seqnum);
            if (numResponses == null) {
                Logger.LOG("Received response for unknown request with sequence number: " + seqnum);
                return;
            }

            numResponses.put(response, numResponses.getOrDefault(response, 0) + 1);

            if (numResponses.get(response) == Config.ALLOWED_FAILURES + 1) {
                System.out.println("Received f+1 of the following(" + seqnum + "):\n-> " + response.toString());
            }

        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize response: " + e.getMessage());
        }
    }
}