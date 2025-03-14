package pt.tecnico.ulisboa;

import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.AppendReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;    

public class NodeMessageHandler<T extends RequiresEquals> implements MessageHandler {
    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;
    
    public NodeMessageHandler(ObservedResource<Queue<T>> txQueue, ConcurrentHashMap<Integer, PublicKey> clientKus) {
        this.clientKus = clientKus;
        this.txQueue = txQueue;
    }

    public void onMessage(int senderid, byte[] message) {
        try {
            BlockchainMessage blockchainMessage = (BlockchainMessage) SerializationUtils.deserializeObject(message);
            BlockchainMessageType type = blockchainMessage.getType();
            switch (type) {
                case REGISTER_REQ:
                    RegisterReq keyReq = (RegisterReq) blockchainMessage;
                    int senderId = keyReq.getSenderId();
                    handleRegisterRequest(senderId, keyReq);
                    break;
                case APPEND_REQ:
                    AppendReq<?> appReq = (AppendReq<?>) blockchainMessage;
                    handleAppendRequest(appReq);
                    break;
                default:
                    Logger.LOG("Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRegisterRequest(int senderId, RegisterReq message) {
        PublicKey ku = CryptoUtils.bytesToPublicKey(message.getKey());
        if (clientKus.containsKey(senderId)) {
            Logger.LOG("Client already registered");
            return;
        }
        clientKus.put(senderId, ku);
    }

    @SuppressWarnings("unchecked")
    public void handleAppendRequest(AppendReq<?> message) {
        String dataToValidate = message.getId().toString() + message.getMessage().toString() + message.getSeqNum().toString();
        Logger.LOG("Data to validate: " + dataToValidate);
        PublicKey clientKU = clientKus.get(message.getId());
        if (clientKU == null) {
            Logger.LOG("Client key not found for id: " + message.getId());
            return;
        }
        if (!CryptoUtils.verifySignature(dataToValidate, message.getSignature(), clientKU)) {
            Logger.LOG("Invalid signature for message: " + message);
            return;
        }
        Logger.LOG("Valid signature for message: " + message.getSeqNum());
        txQueue.getResource().add((T) message);
    }
}