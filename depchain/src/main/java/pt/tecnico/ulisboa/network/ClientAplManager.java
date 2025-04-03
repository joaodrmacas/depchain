package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

import org.hyperledger.besu.datatypes.Address;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.message.FragmentedMessage;
import pt.tecnico.ulisboa.server.Server;
import pt.tecnico.ulisboa.server.ServerMessageHandler;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class ClientAplManager extends AplManager {

    private Server server;
    private ConcurrentHashMap<Integer, PublicKey> clientKus;
    private ConcurrentHashMap<Integer, Address> addresses;

    public ClientAplManager(Server server, String address, Integer port, PrivateKey privateKey,
            ConcurrentHashMap<Integer, PublicKey> clientKus, ConcurrentHashMap<Integer, Address> addresses)
            throws SocketException, IOException {
        super(address, port, privateKey);
        this.server = server;
        this.clientKus = clientKus;
        this.addresses = addresses;
        Logger.LOG("ClientAplManager: " + address + ":" + port);
    }

    @Override
    protected void handleUnknownSender(DatagramPacket packet) {

        Logger.LOG("Creating an apl instance for the new unknown sender.");
        try {
            // KeyMessage message = (KeyMessage) Message.deserialize(packet.getData());

            ServerMessageHandler handler = new ServerMessageHandler(server, clientKus, addresses);

            FragmentedMessage frag = SerializationUtils.deserializeObject(packet.getData());

            int destId = packet.getPort() - Config.DEFAULT_CLIENT_PORT;
            APLImpl apl = createAPL(destId, packet.getAddress().getHostAddress(), packet.getPort(), handler);
            apl.processReceivedPacket(destId, frag.getFragmentData());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
