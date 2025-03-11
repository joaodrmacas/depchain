package pt.tecnico.ulisboa;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.AppendReq;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.protocol.KeyRegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class NodeMessageHandler implements MessageHandler {
    private ConcurrentLinkedQueue<BlockchainMessage> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKeys;
    
    public NodeMessageHandler(ConcurrentLinkedQueue<BlockchainMessage> txQueue, ConcurrentHashMap<Integer, PublicKey> clientKeys) {
        this.txQueue = txQueue;
        this.clientKeys = clientKeys;
    }

    public void onMessage(int senderId, byte[] message) {

        try {
            BlockchainMessage blockchainMessage = (BlockchainMessage) SerializationUtils.deserializeObject(message);
            BlockchainMessageType type = blockchainMessage.getType();
            switch (type) {
                case APPEND_REQ:
                    //TODO: dá para saber que tipo vem? - massas
                    AppendReq<?> appReq = (AppendReq<?>) blockchainMessage;
                    handleAppendRequest(appReq);
                    break;
                case KEY_REGISTER_REQ:
                    KeyRegisterReq keyReq = (KeyRegisterReq) blockchainMessage;
                    handleKeyRegisterRequest(senderId, keyReq);
                    break;
                default:
                    Logger.LOG("Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public void handleAppendRequest(AppendReq<?> message) {
        txQueue.add(message);
    }

    public void handleKeyRegisterRequest(int senderId, KeyRegisterReq message) {
        clientKeys.put(senderId, CryptoUtils.bytesToPublicKey(message.getKey()));
    }
}