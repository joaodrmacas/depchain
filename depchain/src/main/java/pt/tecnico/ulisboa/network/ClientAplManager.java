package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.NodeMessageHandler;
import pt.tecnico.ulisboa.protocol.RegisterReq;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class ClientAplManager<T extends RequiresEquals> extends AplManager {

    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;

    public ClientAplManager(String address, Integer port, PrivateKey privateKey, ObservedResource<Queue<T>> txQueue,
            ConcurrentHashMap<Integer, PublicKey> clientKus) throws SocketException, IOException {
        super(address, port, privateKey);
        Logger.LOG("ClientAplManager: " + address + ":" + port);
    }

    @Override
    protected void handleUnknownSender(DatagramPacket packet) {
        Logger.LOG("Unknown sender: " + packet.getAddress().getHostAddress() + ":" + packet.getPort());

        try {
            RegisterReq msg = (RegisterReq) SerializationUtils.deserializeObject(packet.getData());

            int clientid = msg.getSenderId();

            NodeMessageHandler<T> handler = new NodeMessageHandler<>(txQueue, clientKus);

            PublicKey ku = CryptoUtils.bytesToPublicKey(msg.getKey());
            clientKus.put(clientid, ku);

            createAPL(clientid, packet.getAddress().getHostAddress(), packet.getPort(), ku, handler);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
