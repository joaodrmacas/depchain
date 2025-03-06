package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.SecretKey;

import pt.tecnico.ulisboa.Config;
import pt.tecnico.ulisboa.crypto.CryptoUtils;
import pt.tecnico.ulisboa.network.message.AckMessage;
import pt.tecnico.ulisboa.network.message.DataMessage;
import pt.tecnico.ulisboa.network.message.KeyMessage;
import pt.tecnico.ulisboa.network.message.Message;

public class AuthenticatedPerfectLinkImpl implements AuthenticatedPerfectLink {
    private final DatagramSocket socket;
    private MessageHandler messageHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Map of pending messages, indexed by the destination ID and sequence number
    private final ConcurrentMap<String, Message> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Set<Long>> receivedMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Long> nextSeqNum = new ConcurrentHashMap<>();

    private static final int BUFFER_SIZE = 4096;

    private final PrivateKey privateKey;
    private final ConcurrentMap<Integer, PublicKey> publicKeys = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, SecretKey> secretKeys = new ConcurrentHashMap<>();
    private final int nodeId;

    private int waitingFor = -1;
    private final Lock messageReceivedLock = new ReentrantLock();
    private final Condition messageReceived = messageReceivedLock.newCondition();

    public AuthenticatedPerfectLinkImpl(int nodeId, PrivateKey privateKey, Map<Integer, PublicKey> publicKeys)
            throws SocketException, GeneralSecurityException, IOException {

        String address = Config.id2addr.get(nodeId);
        String destination = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);

        // print ip and port
        System.out.println("IP: " + destination + " Port: " + port);

        this.socket = new DatagramSocket(port, InetAddress.getByName(destination));
        this.nodeId = nodeId;

        this.privateKey = privateKey;
        this.publicKeys.putAll(publicKeys);

        startListening();

        startRetransmissionScheduler();
        System.out.println("AuthenticatedPerfectLink started on port: " + port + " with node ID: " + nodeId);
    }

    public void receivedFrom(int senderId) {
        messageReceivedLock.lock();
        try {
            if (senderId == waitingFor) {
                this.waitingFor = senderId;
                messageReceived.signal();
            }
        } finally {
            messageReceivedLock.unlock();
        }
    }

    public boolean waitFor(int senderId) {
        messageReceivedLock.lock();
        try {
            if (this.waitingFor != senderId) {
                messageReceived.await(Config.LINK_TIMEOUT, TimeUnit.MILLISECONDS);
                if (this.waitingFor != senderId) {
                    return false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            messageReceivedLock.unlock();
        }

        this.waitingFor = -1;
        return true;
    }

    public void send(int destId, byte[] message) {
        // Get sequence number for this message
        long seqNum = nextSeqNum.getOrDefault(destId, 1L);
        nextSeqNum.put(destId, seqNum + 1);

        try {
            SecretKey secretKey = getOrGenerateSecretKey(destId);
            String data = Integer.toString(nodeId) + Integer.toString(destId) + Arrays.toString(message)
                    + Long.toString(seqNum);
            byte[] hmac = CryptoUtils.generateHMAC(data, secretKey);

            DataMessage dataMsg = new DataMessage(message, seqNum, hmac);

            // Store the message in the pending list
            String messageId = destId + ":" + seqNum;
            pendingMessages.put(messageId, dataMsg);

            sendUdpPacket(destId, dataMsg.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    private void startListening() {
        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Process received packet
                    byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                    String senderaddr = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    int senderId = Config.addr2id.get(senderaddr);

                    receivedFrom(senderId);

                    // print the sender
                    System.out.println("Received message from: " + senderId);

                    processReceivedPacket(senderId, data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processReceivedPacket(int senderId, byte[] data) {
        // print in function
        System.out.println("Processing message from: " + senderId);
        Message message = Message.deserialize(data);
        if (message == null)
            return;

        System.out.println("Message typeeeeeeeeeeeeeee: " + message.getType());

        switch (message.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                DataMessage dataMessage = (DataMessage) message;
                handleData(dataMessage, senderId);
                break;
            case Message.ACK_MESSAGE_TYPE:
                AckMessage ackMessage = (AckMessage) message;
                handleACK(ackMessage, senderId);
                break;
            case Message.KEY_MESSAGE_TYPE:
                KeyMessage keyMessage = (KeyMessage) message;
                handleKey(keyMessage, senderId);
                break;
            default:
                System.err.println("Unknown message type: " + message.getType());
        }
    }

    private void handleKey(KeyMessage keyMessage, int senderId) {
        // print in function
        System.out.println("Received key message from: " + senderId);
        SecretKey secretKey = null;
        try {
            secretKey = CryptoUtils.decryptSymmetricKey(keyMessage.getContent(), privateKey);
        } catch (Exception e) {
            System.err.println("Failed to decrypt symmetric key: " + e.getMessage());
            return;
        }

        secretKeys.put(senderId, secretKey);

        // print the key
        System.out.println("Received key: " + secretKey);
        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(senderId, keyMessage.getSeqNum());
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(senderId, keyMessage.getSeqNum())) {
            return;
        }
        System.out.println("map: " + secretKeys);
    }

    private void handleACK(AckMessage ackMessage, int senderId) {
        byte[] content = ackMessage.getContent();
        long seqNum = ackMessage.getSeqNum();
        byte[] hmac = ackMessage.getMac();

        if (!verifyHMAC(senderId, content, seqNum, hmac)) {
            System.err.println("Authentication failed for ACK from " + senderId);
            return;
        }

        // Remove the message from the pending list
        String messageId = senderId + ":" + seqNum;

        // print when receiving ack foe unsent message
        if (!pendingMessages.containsKey(messageId)) {
            System.out.println("Received ACK for unsent message: " + messageId);
        }

        pendingMessages.remove(messageId);
    }

    private void handleData(DataMessage dataMessage, int senderId) {
        byte[] content = dataMessage.getContent();
        long seqNum = dataMessage.getSeqNum();
        byte[] hmac = dataMessage.getMac();

        if (!verifyHMAC(senderId, content, seqNum, hmac)) {
            System.err.println("Authentication failed for message from " + senderId);
            return;
        }

        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(senderId, seqNum);
        } catch (Exception e) {
            System.err.println("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(senderId, seqNum)) {
            return;
        }

        // Deliver to application if not a duplicate and signature is valid
        if (messageHandler != null) {
            messageHandler.onMessage(senderId, content);
        }
    }

    private boolean isDuplicate(int senderId, long seqNum) {
        Set<Long> received = receivedMessages.computeIfAbsent(senderId, k -> ConcurrentHashMap.newKeySet());
        return !received.add(seqNum);
    }

    private void sendAuthenticatedAcknowledgment(int destId, long seqNum) throws Exception {
        try {

            SecretKey secretKey = getSecretKey(destId);
            String data = Integer.toString(nodeId) + Integer.toString(destId) + Arrays.toString(new byte[0]) +  Long.toString(seqNum);
            byte[] hmac = CryptoUtils.generateHMAC(data, secretKey);

            // Create authenticated ACK message
            AckMessage ackMessage = new AckMessage(seqNum, hmac);

            // Send it
            sendUdpPacket(destId, ackMessage.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send ACK: " + e.getMessage());
            throw e;
        }
    }

    private void sendUdpPacket(int destId, byte[] data) {
        try {
            String[] parts = Config.id2addr.get(destId).split(":");
            InetAddress address = InetAddress.getByName(parts[0]); // IP address of the destination
            int port = Integer.parseInt(parts[1]);

            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRetransmissionScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            // loop through all pending messages and retransmit if necessary
            pendingMessages.forEach((messageId, message) -> {
                if (message.getCounter() >= message.getCooldown()) {
                    int nodeId = Integer.parseInt(messageId.split(":")[0]);

                    System.out.println("Retransmitting message: " + messageId + "\nWaited cooldown: "
                            + message.getCooldown() * 0.5 + "s");
                    try {
                        sendUdpPacket(nodeId, message.serialize());
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

    private boolean verifyHMAC(int senderId, byte[] content, long seqNum, byte[] hmac) {
        try {
            SecretKey secretKey = getSecretKey(senderId);

            String data = Integer.toString(senderId) + Integer.toString(nodeId) + Arrays.toString(content)
                    + Long.toString(seqNum);

            return CryptoUtils.verifyHMAC(data, secretKey, hmac);

        } catch (Exception e) {
            System.err.println("Failed to verify HMAC: " + e.getMessage());
            return false;
        }
    }

    private SecretKey getOrGenerateSecretKey(int destId) {
        //print 
        System.out.println("Getting or generating secret key for: " + destId);
        SecretKey secretKey = getSecretKey(destId);
        if (secretKey == null) {
            System.out.println("I am here"); 
            secretKey = generateAndShareSecretKey(destId);
            secretKeys.put(destId, secretKey);
        }
        return secretKey;
    }

    private SecretKey getSecretKey(int destId) {
        return secretKeys.get(destId);
    }

    private SecretKey generateAndShareSecretKey(int destId) {

        try {

            PublicKey publicKey = publicKeys.get(destId);
            SecretKey secretKey = CryptoUtils.generateSymmetricKey();
            byte[] encryptedKey = CryptoUtils.encryptSymmetricKey(secretKey, publicKey);

            long seqNum = nextSeqNum.getOrDefault(destId, 1L);
            nextSeqNum.put(destId, seqNum + 1);

            KeyMessage keyMessage = new KeyMessage(encryptedKey, seqNum);
            // print sending key
            System.out.println("Sending key: " + secretKey);
            sendUdpPacket(destId, keyMessage.serialize());

            return secretKey;

        } catch (Exception e) {
            System.err.println("Failed to generate and share secret key: " + e.getMessage());
            return null;
        }
    }
}