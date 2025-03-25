package pt.tecnico.ulisboa.consensus.message;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.network.MessageHandler;

public class ConsensusMessageHandler<T extends RequiresEquals> implements MessageHandler {
    
    private Map<Integer, ObservedResource<Queue<ConsensusMessage<T>>>> receivedMessages;

    public ConsensusMessageHandler(Map<Integer, ObservedResource<Queue<ConsensusMessage<T>>>> receivedMessages) {
        this.receivedMessages = receivedMessages;
    }

    @Override
    public void onMessage(int senderId, byte[] message) {
        try {
            Object deserializedObject = SerializationUtils.deserializeObject(message);
            if (deserializedObject instanceof ConsensusMessage<?>) {
                @SuppressWarnings("unchecked")
                ConsensusMessage<T> consensusMessage = (ConsensusMessage<T>) deserializedObject;

                receivedMessages.get(senderId).getResource().add(consensusMessage);
                receivedMessages.get(senderId).notifyChange();

                Logger.LOG("Received consensus message of type: " + consensusMessage.getType() + " from node: " + senderId);
            } 
        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize message in onMessage: " + e.getMessage());
        }
    }

    public Map<Integer, ObservedResource<Queue<ConsensusMessage<T>>>> getReceivedMessages() {
        return receivedMessages;
    }
}
