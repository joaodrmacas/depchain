package pt.tecnico.ulisboa.client;

import java.util.concurrent.CountDownLatch;
import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.Logger;
import java.io.IOException;
import pt.tecnico.ulisboa.protocol.*;


public class ClientMessageHandler<T> implements MessageHandler {
    private CountDownLatch responseLatch;
    private BlockchainMessage<T> receivedResponse;
    
    public ClientMessageHandler() {
        this.responseLatch = new CountDownLatch(1);
        this.receivedResponse = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(int senderId, byte[] message) {
        try {
            BlockchainResponse<T> response = (BlockchainResponse<T>) SerializationUtils.deserializeObject(message);
            Logger.LOG("Received response from node " + senderId + ": " + response);
            
            this.receivedResponse = response;
            
            responseLatch.countDown();
        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize response: " + e.getMessage());
        }
    } 

    public BlockchainResponse<T> waitForResponse() throws InterruptedException {
        responseLatch.await();
        return (BlockchainResponse<T>) receivedResponse;
    }

    public void reset() {
        this.responseLatch = new CountDownLatch(1);
        this.receivedResponse = null;
    }
}