package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.utils.Logger;

public abstract class AplManager {
    private final DatagramSocket socket;
    private final Map<Integer, APLImpl> aplInstances = new ConcurrentHashMap<>();
    private final Map<String, Integer> senderIdMap = new ConcurrentHashMap<>();
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

    public APLImpl createAPL(Integer destId, String destAddress, Integer destPort, PublicKey destPublicKey,
            MessageHandler msgHandler) throws Exception {
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

    public Integer getSenderId(String address, Integer port) {
        return senderIdMap.get(address + ":" + port);
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
            Logger.LOG(
                    "No APL instance found for destination ID: " + destId + " when trying to send message: " + message);
        }
    }

    public void sendWithTimeout(int destId, Serializable message, int timeout) {
        APLImpl apl = aplInstances.get(destId);
        if (apl != null) {
            apl.sendWithTimeout(message, timeout);
        } else {
            Logger.LOG(
                    "No APL instance found for destination ID: " + destId + " when trying to send message: " + message);
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
                    // TODO: this might be spoofed no?
                    String senderAddr = packet.getAddress().getHostAddress() + ":" + packet.getPort();

                    Integer senderId = senderIdMap.get(senderAddr);
                    if (senderId == null) {
                        Logger.LOG("Received packet from unknown sender: " + senderAddr + "Creating new apl instance");
                        handleUnknownSender(packet);
                    }

                    Logger.LOG("Received packet from sender ID: " + senderId);

                    // Get the APL for this sender and dispatch the data of the packet
                    APLImpl apl = aplInstances.get(senderId);
                    if (apl != null) {
                        byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                        apl.handleData(senderId, data);
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

    protected abstract void handleUnknownSender(DatagramPacket packet);

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
