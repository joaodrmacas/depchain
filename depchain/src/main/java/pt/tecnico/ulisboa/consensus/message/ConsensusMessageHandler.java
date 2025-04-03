package pt.tecnico.ulisboa.consensus.message;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.utils.types.ObservedResource;
import pt.tecnico.ulisboa.network.MessageHandler;

public class ConsensusMessageHandler implements MessageHandler {
    
    private Map<Integer, ObservedResource<Queue<ConsensusMessage>>> receivedMessages;

    public ConsensusMessageHandler(Map<Integer, ObservedResource<Queue<ConsensusMessage>>> receivedMessages) {
        this.receivedMessages = receivedMessages;
    }

    @Override
    public void onMessage(int senderId, byte[] message) {
        try {
            Object deserializedObject = SerializationUtils.deserializeObject(message);
            if (deserializedObject instanceof ConsensusMessage) {
                ConsensusMessage consensusMessage = (ConsensusMessage) deserializedObject;

                receivedMessages.get(senderId).getResource().add(consensusMessage);
                receivedMessages.get(senderId).notifyChange();

                Logger.LOG("Received consensus message of type: " + consensusMessage.getType() + " from node: " + senderId);
            } 
        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize message in onMessage: " + e.getMessage());
        }
    }

    public Map<Integer, ObservedResource<Queue<ConsensusMessage>>> getReceivedMessages() {
        return receivedMessages;
    }
}
