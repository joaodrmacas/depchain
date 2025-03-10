package pt.tecnico.ulisboa.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class BlockchainMessageHandler implements MessageHandler {
    private ConcurrentHashMap<Long, HashMap<BlockchainMessage, Integer>> receivedResponses;

    public BlockchainMessageHandler(ConcurrentHashMap<Long, HashMap<BlockchainMessage, Integer>> receivedResponses) {
        this.receivedResponses = receivedResponses;
    }

    @Override
    public void onMessage(int senderId, byte[] message) {
        try {
            BlockchainMessage response = (BlockchainMessage) SerializationUtils.deserializeObject(message);
            long seqnum = response.getSeqNum();

            if (this.receivedResponses.containsKey(seqnum)) {
                Map<BlockchainMessage, Integer> responseMap = this.receivedResponses.get(seqnum);
                if (responseMap.containsKey(response)) {
                    responseMap.put(response, responseMap.get(response) + 1);
                } else {
                    responseMap.put(response, 1);
                }
            } else {
                HashMap<BlockchainMessage, Integer> responseMap = new HashMap<>();
                responseMap.put(response, 1);
                this.receivedResponses.put(seqnum, responseMap);
            }

        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize response: " + e.getMessage());
        }
    }

    public BlockchainMessage getResponse(long seqNum) {
        Map<BlockchainMessage, Integer> responseMap = this.receivedResponses.get(seqNum);
        if (responseMap == null) {
            return null;
        }

        BlockchainMessage response = null;
        int max = 0;
        for (Map.Entry<BlockchainMessage, Integer> entry : responseMap.entrySet()) {
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
