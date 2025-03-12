package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.network.message.AckMessage;
import pt.tecnico.ulisboa.network.message.DataMessage;
import pt.tecnico.ulisboa.network.message.KeyMessage;
import pt.tecnico.ulisboa.network.message.Message;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.SerializationUtils;

public class APLImpl implements APL {
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ConcurrentHashMap<Long, Message> pendingMessages = new ConcurrentHashMap<>();

    private long nextSeqNum = 1L;

    private final PublicKey destPublicKey;
    private SecretKey secretKey;

    String destAddress;
    Integer destPort;

    // Constructor that accepts an existing socket
    public APLImpl(String destAddress, Integer destPort, PublicKey destPublicKey, DatagramSocket socket)
            throws GeneralSecurityException, IOException {
        this.socket = socket;
        this.destAddress = destAddress;
        this.destPort = destPort;
        this.destPublicKey = destPublicKey;

        startRetransmissionScheduler();

        Logger.LOG("APL initialized");
    }

    // // For unit tests
    // public APLImpl(int nodeId, int destId, PrivateKey privateKey, PublicKey destPublicKey,
    //         MessageHandler messageHandler)
    //         throws SocketException, GeneralSecurityException, IOException {
    //     this(nodeId, destId, privateKey, destPublicKey);
    //     this.messageHandler = messageHandler;
    // }

    // // For unit tests with existing socket
    // public APLImpl(String destAddress, Integer destPort, PrivateKey privateKey, PublicKey destPublicKey,
    //         MessageHandler messageHandler, DatagramSocket socket)
    //         throws GeneralSecurityException, IOException {
    //     this(destAddress, destPort, privateKey, destPublicKey, socket, messageHandler);
    // }

    public void sendKeyMessage(KeyMessage keyMessage) {
        long seqNum = nextSeqNum++;
        pendingMessages.put(seqNum, keyMessage);
        Logger.LOG("Sending key message: " + keyMessage);
        sendUdpPacket(keyMessage.serialize());
    }

    public void sendDataMessage(DataMessage dataMsg) {
        long seqNum = nextSeqNum++;
        pendingMessages.put(seqNum, dataMsg);
        Logger.LOG("Sending message: " + dataMsg);
        sendUdpPacket(dataMsg.serialize());
    }

    public void send(Serializable obj) {
        byte[] messageBytes = null;
        try {
            messageBytes = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            System.err.println("Failed to serialize message: " + e.getMessage());
            return;
        }
        send(messageBytes);
    }

    public void send(byte[] message) {
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum++;

        try {
            byte[] hmac = generateHMAC(message, seqNum, secretKey);

            DataMessage dataMsg = new DataMessage(message, seqNum, hmac);

            // Store the message in the pending list
            pendingMessages.put(seqNum, dataMsg);

            Logger.LOG("Sending message: " + dataMsg);
            sendUdpPacket(dataMsg.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void sendAuthenticatedAcknowledgment(long seqNum) throws Exception {
        try {
            SecretKey secretKey = getSecretKey();

            byte[] hmac = generateHMAC(new byte[0], seqNum, secretKey);

            AckMessage ackMessage = new AckMessage(seqNum, hmac);

            sendUdpPacket(ackMessage.serialize());
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send ACK", e);
            throw e;
        }
    }

    private void sendUdpPacket(byte[] data) {
        try {
            InetAddress address = InetAddress.getByName(destAddress);
            int port = destPort;

            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            Logger.ERROR("Failed to send UDP packet", e);
        }
    }


    private void startRetransmissionScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            // loop through all pending messages and retransmit if necessary
            pendingMessages.forEach((seqNum, message) -> {
                if (message.getCounter() >= message.getCooldown()) {
                    Logger.LOG("Retransmitting message with seqNum: " + seqNum +
                            "\nWaited cooldown: " + message.getCooldown() * 0.5 + "s");
                    try {
                        sendUdpPacket(message.serialize());
                    } catch (Exception e) {
                        System.err.println("Failed to retransmit message: " + e.getMessage());
                    }
                    message.setCounter(1);
                    message.doubleCooldown(); // Exponential backoff
                } else {
                    message.incrementCounter();
                }
            });
        }, Config.RETRANSMISSION_TIME, Config.RETRANSMISSION_TIME, TimeUnit.MILLISECONDS);
    }

    private byte[] generateHMAC(byte[] content, long seqNum, SecretKey secretKey) {
        try {
            String data = Arrays.toString(content) + Long.toString(seqNum);
            return CryptoUtils.generateHMAC(data, secretKey);
        } catch (Exception e) {
            System.err.println("Failed to generate HMAC: " + e.getMessage());
            return null;
        }
    }

    private SecretKey getOrGenerateSecretKey() {
        Logger.LOG("Getting or generating secret key");
        SecretKey secretKey = getSecretKey();
        if (secretKey == null) {
            Logger.LOG("Generating new secret key");
            secretKey = generateAndShareSecretKey();
            this.secretKey = secretKey;
        }
        return secretKey;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public ConcurrentHashMap<Long, Message> getPendingMessages() {
        return pendingMessages;
    }

    private SecretKey generateAndShareSecretKey() {
        try {
            SecretKey secretKey = CryptoUtils.generateSymmetricKey();
            byte[] encryptedKey = CryptoUtils.encryptSymmetricKey(secretKey, destPublicKey);

            long seqNum = nextSeqNum++;

            KeyMessage keyMessage = new KeyMessage(encryptedKey, seqNum);
            pendingMessages.put(seqNum, keyMessage);

            Logger.LOG("Sending key: " + secretKey);
            sendUdpPacket(keyMessage.serialize());

            return secretKey;
        } catch (Exception e) {
            System.err.println("Failed to generate and share secret key: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        scheduler.shutdown();
    }
}
