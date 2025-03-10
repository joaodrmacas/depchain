package pt.tecnico.ulisboa.consensus;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.consensus.message.*;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.network.MessageHandler;

public class ConsensusMessageHandler<T extends RequiresEquals> implements MessageHandler {
    
    private final Queue<ConsensusMessage<T>> receivedMessages;

    public ConsensusMessageHandler() {
        this.receivedMessages = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void onMessage(int senderId, byte[] message) {
        try {
            ConsensusMessage<T> deserializedObject = SerializationUtils.deserializeObject(message);
            if (deserializedObject instanceof ConsensusMessage<?>) {
                ConsensusMessage<T> consensusMessage = (ConsensusMessage<T>) deserializedObject;
                
                receivedMessages.add(consensusMessage);
                Logger.LOG("Received consensus message of type: " + consensusMessage.getType() + " from node: " + senderId);
                
                switch (consensusMessage.getType()) {
                    case READ:
                        handleReadMessage(senderId, (ReadMessage<T>) consensusMessage);
                        break;
                    case STATE:
                        handleStateMessage(senderId, (StateMessage<T>) consensusMessage);
                        break;
                    case WRITE:
                        handleWriteMessage(senderId, (WriteMessage<T>) consensusMessage);
                        break;
                    case ACCEPT:
                        handleAcceptMessage(senderId, (AcceptMessage<T>) consensusMessage);
                        break;
                    case COLLECTED:
                        handleCollectedMessage(senderId, (CollectedMessage<T>) consensusMessage);  
                        break;
                    default:
                        Logger.LOG("Unknown consensus message type: " + consensusMessage.getType());
                }
            } 
        } catch (IOException | ClassNotFoundException e) {
            Logger.LOG("Failed to deserialize message in onMessage: " + e.getMessage());
        }
    }

    private void handleReadMessage(int senderId, ReadMessage<T> readMessage) {
        Logger.LOG("Received READ message from node: " + senderId);
    }

    private void handleStateMessage(int senderId, StateMessage<T> stateMessage) {
        Logger.LOG("Received STATE message from node: " + senderId);
    }

    private void handleWriteMessage(int senderId, WriteMessage<T> writeMessage) {
        WriteTuple<T> t =
            new WriteTuple<T>(writeMessage.getValue(), writeMessage.getEpochNumber());
        Logger.LOG("Received WRITE message from node: " + senderId + " with tuple: " + t);
    }

    private void handleAcceptMessage(int senderId, AcceptMessage<T> acceptMessage) {
        Logger.LOG("Received ACCEPT message from node: " + senderId);
    }

    private void handleCollectedMessage(int senderId, CollectedMessage<T> collectedMessage) {
        Logger.LOG("Received COLLECTED message from node: " + senderId);
    }    

    public void reset() {
        receivedMessages.clear();
    }

    public Queue<ConsensusMessage<T>> getReceivedMessages() {
        return receivedMessages;
    }
}
