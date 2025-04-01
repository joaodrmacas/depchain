package pt.tecnico.ulisboa.client;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class ClientMessageHandler implements MessageHandler {
    private long requestSeqNum;
    private ConcurrentHashMap<ClientResp, Integer> currentRequestResponses;
    private CountDownLatch responseLatch;
    private AtomicReference<ClientResp> acceptedResponse;

    public ClientMessageHandler(long requestSeqNum,
            ConcurrentHashMap<ClientResp, Integer> currentRequestResponses,
            CountDownLatch responseLatch,
            AtomicReference<ClientResp> acceptedResponse) {
        this.currentRequestResponses = currentRequestResponses;
        this.requestSeqNum = requestSeqNum;
        this.responseLatch = responseLatch;
        this.acceptedResponse = acceptedResponse;
    }

    public void updateForNewRequest(long requestSeqNum, CountDownLatch newLatch) {
        this.requestSeqNum = requestSeqNum;
        this.responseLatch = newLatch;
    }

    @Override
    public void onMessage(int senderid, byte[] message) {
        try {
            ClientResp response = (ClientResp) SerializationUtils.deserializeObject(message);
            long seqnum = response.getCount();
            if (this.requestSeqNum != seqnum) {
                Logger.LOG("Ignoring response with wrong sequence number in client on Message: " + seqnum
                        + "; expected: " + this.requestSeqNum);
                return;
            }

            currentRequestResponses.put(response, currentRequestResponses.getOrDefault(response, 0) + 1);
            acceptedResponse.set(response);

            // check if we have enough responses
            if (currentRequestResponses.get(response) > Config.ALLOWED_FAILURES) {
                Logger.LOG("Received enough equal responses for request with sequence number " +
                        seqnum);

                acceptedResponse.set(response);
                responseLatch.countDown();
            }

        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize response: " + e.getMessage());
        }
    }
}