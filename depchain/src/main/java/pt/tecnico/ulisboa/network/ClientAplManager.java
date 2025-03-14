package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.NodeMessageHandler;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;
public class ClientAplManager<T extends RequiresEquals> extends AplManager {
 
    private ObservedResource<Queue<T>> txQueue;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;

    public ClientAplManager(String address, Integer port, PrivateKey privateKey, ObservedResource<Queue<T>> txQueue,
            ConcurrentHashMap<Integer, PublicKey> clientKus) throws SocketException, IOException {
        super(address, port, privateKey);
        this.txQueue = txQueue;
        this.clientKus = clientKus;
        Logger.LOG("ClientAplManager: " + address + ":" + port);
    }

    @Override
    protected void handleUnknownSender(DatagramPacket packet) {

        Logger.LOG("Creating an apl instance for the new unknown sender.");
        try {
            // KeyMessage message = (KeyMessage) Message.deserialize(packet.getData());

            NodeMessageHandler<T> handler = new NodeMessageHandler<>(txQueue, clientKus);

            // TODO: change this shi
            APLImpl apl = createAPL(1, packet.getAddress().getHostAddress(), packet.getPort(), handler);
            apl.processReceivedPacket(1,packet.getData());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
