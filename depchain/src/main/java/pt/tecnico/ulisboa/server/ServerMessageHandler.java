package pt.tecnico.ulisboa.server;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import org.hyperledger.besu.datatypes.Address;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.protocol.BlockchainMessage.BlockchainMessageType;
import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class ServerMessageHandler implements MessageHandler {
    private Server server;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;
    private ConcurrentHashMap<Integer, Address> clientAddresses;

    public ServerMessageHandler(Server server, ConcurrentHashMap<Integer, PublicKey> clientKus,
            ConcurrentHashMap<Integer, Address> clientAddresses) {
        this.server = server;
        this.clientKus = clientKus;
        this.clientAddresses = clientAddresses;
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
                case CLIENT_REQ:
                    handleClientRequest((ClientReq) blockchainMessage);
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
        if (senderId >= 0) {
            Logger.LOG("Client id must be negative");
            return;
        }

        clientKus.put(senderId, ku);
        clientAddresses.put(senderId, ContractUtils.generateAddressFromId(senderId));
    }

    public void handleClientRequest(ClientReq message) {
        PublicKey clientKU = clientKus.get(message.getSenderId());
        if (clientKU == null) {
            Logger.LOG("Client key not found for id: " + message.getSenderId());
            return;
        }
        if (!message.verifySignature(clientKU)) {
            Logger.LOG("Invalid signature for message: " + message);
            return;
        }
        Logger.LOG("Valid signature for message: " + message.getCount());

        server.handleClientRequest(message);
    }
}