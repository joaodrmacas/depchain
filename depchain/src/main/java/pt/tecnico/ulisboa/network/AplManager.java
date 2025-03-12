package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.utils.GeneralUtils;
import pt.tecnico.ulisboa.utils.Logger;

public class AplManager {
    private final DatagramSocket socket;
    private final Map<Integer, APLImpl> aplInstances = new ConcurrentHashMap<>();
    private final PrivateKey privateKey;
    private boolean isRunning = true;

    public AplManager(String address, Integer port, PrivateKey privateKey) throws SocketException, IOException {
        this.privateKey = privateKey;

        Logger.LOG("Creating shared socket - IP: " + address + " Port: " + port);
        this.socket = new DatagramSocket(port, InetAddress.getByName(address));

        startMessageDispatcher();
    }

    public AplManager(String address, Integer port) throws SocketException, IOException {
        this(address, port, null);
    }

    public APLImpl createAPL(Integer destId, String destAddress, Integer destPort, PublicKey destPublicKey, MessageHandler msgHandler) throws Exception {
        if (aplInstances.containsKey(destId)) {
            Logger.LOG("APL instance already exists for destination ID: " + destId + ". Returning existing instance.");
            return aplInstances.get(destId);
        }

        APLImpl apl = new APLImpl(destAddress, destPort, privateKey, destPublicKey, socket, msgHandler);
        aplInstances.put(destId, apl);

        return apl;
    }

    public APLImpl getAPL(int destId) {
        return aplInstances.get(destId);
    }

    public void removeAPL(int destId) {
        aplInstances.remove(destId);
    }

    public void send(int destId, byte[] message) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.send(message);
        } else {
            Logger.LOG("No APL instance found for destination ID: " + destId);
        }
    }

    public void send(int destId, Serializable message) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.send(message);
        } else {
            Logger.LOG("No APL instance found for destination ID: " + destId);
        }
    }

    // Start the message dispatcher thread
    private void startMessageDispatcher() {
        new Thread(() -> {
            byte[] buffer = new byte[Config.BUFFER_SIZE];

            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Determine the sender
                    String senderAddr = packet.getAddress().getHostAddress() + ":" + packet.getPort();

                    if (senderAddr== null) {
                        Logger.LOG("Received packet from unknown sender: " + senderAddr);
                        continue;
                    }

                    // Get the APL for this sender and dispatch the packet
                    APLImpl apl = aplInstances.get(senderId);
                    if (apl != null) {
                        apl.handlePacket(packet);
                    } else {
                        Logger.LOG("No APL instance found for sender ID: " + senderId);
                    }

                } catch (Exception e) {
                    if (isRunning) {
                        Logger.ERROR("Error receiving packet", e);
                    }
                }
            }
        }).start();
    }

    // Clean shutdown
    public void close() {
        isRunning = false;

        // Close all APL instances
        for (APLImpl apl : aplInstances.values()) {
            apl.close();
        }

        aplInstances.clear();

        // Close the socket
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}