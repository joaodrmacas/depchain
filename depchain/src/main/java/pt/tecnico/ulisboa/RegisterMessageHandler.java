package pt.tecnico.ulisboa;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;    

public class RegisterMessageHandler implements MessageHandler {

    private ConcurrentHashMap<Integer, PublicKey> clientKus;
    
    public RegisterMessageHandler(ConcurrentHashMap<Integer, PublicKey> clientKus) {
        this.clientKus = clientKus;
    }

    public void onMessage(int senderId, byte[] message) {
        try {
            BlockchainMessage blockchainMessage = (BlockchainMessage) SerializationUtils.deserializeObject(message);
            BlockchainMessageType type = blockchainMessage.getType();
            switch (type) {
                case REGISTER_REQ:
                    RegisterReq keyReq = (RegisterReq) blockchainMessage;
                    handleRegisterRequest(senderId, keyReq);
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
}