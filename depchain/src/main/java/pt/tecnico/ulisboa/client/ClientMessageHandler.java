package pt.tecnico.ulisboa.client;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.protocol.*;
import pt.tecnico.ulisboa.Config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMessageHandler<T> implements MessageHandler {
    // for each INteger (response identifier) we have a map of BlockchainResponse and Integer (times that response was received)
    private ConcurrentHashMap<Long, ConcurrentHashMap<BlockchainResponse, Integer>> receivedResponses;

    public ClientMessageHandler() {
        this.receivedResponses = new ConcurrentHashMap<Long, ConcurrentHashMap<BlockchainResponse, Integer>>();
    }

    @Override
    public void onMessage(int senderId, byte[] message) {
        try {
            BlockchainResponse response = (BlockchainResponse) SerializationUtils.deserializeObject(message);

            long seqnum = response.getSeqNum();

            if (this.receivedResponses.containsKey(seqnum)) {
                Map<BlockchainResponse, Integer> responseMap = this.receivedResponses.get(seqnum);
                if (responseMap.containsKey(response)) {
                    responseMap.put(response, responseMap.get(response) + 1);
                } else {
                    responseMap.put(response, 1);
                }
            } else {
                ConcurrentHashMap<BlockchainResponse, Integer> responseMap = new ConcurrentHashMap<>();
                responseMap.put(response, 1);
                this.receivedResponses.put(seqnum, responseMap);
            }


        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize response: " + e.getMessage());
        }
    }

    public BlockchainResponse getResponse(long seqNum) {
        Map<BlockchainResponse, Integer> responseMap = this.receivedResponses.get(seqNum);
        if (responseMap == null) {
            return null;
        }

        BlockchainResponse response = null;
        int max = 0;
        for (Map.Entry<BlockchainResponse, Integer> entry : responseMap.entrySet()) {
            if (entry.getValue() > max) {
                response = entry.getKey();
                max = entry.getValue();
            }
        }
        if (max >= Config.ALLOWED_FAILURES + 1) {
            return response;
        } else {
            Logger.LOG("Failed to get response for seqNum " + seqNum + " with " + max + " responses");
            return null;
        }
    }
}
