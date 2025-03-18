package pt.tecnico.ulisboa.network;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
    private MessageHandler messageHandler;
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ConcurrentHashMap<Long, Message> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> receivedMessages = new ConcurrentHashMap<>();

    private AtomicLong nextSeqNum = new AtomicLong(1);
    private AtomicLong lastReceivedSeqNum = new AtomicLong(0);

    private final PrivateKey privateKey;
    private final PublicKey destPublicKey;
    private SecretKey secretKey;

    InetAddress nodeAddress;
    Integer nodePort;

    InetAddress destAddress;
    Integer destPort;

    // Constructor that accepts an existing socket
    public APLImpl(String nodeAddress, Integer nodePort, String destAddress, Integer destPort, PrivateKey privateKey,
            PublicKey destPublicKey, DatagramSocket socket, MessageHandler messageHandler)
            throws GeneralSecurityException, IOException {
        this.nodeAddress = InetAddress.getByName(nodeAddress);
        this.nodePort = nodePort;
        this.destAddress = InetAddress.getByName(destAddress);
        this.destPort = destPort;
        this.socket = socket;
        this.privateKey = privateKey;
        this.destPublicKey = destPublicKey;
        setMessageHandler(messageHandler);

        startRetransmissionScheduler();
    }

    // // For unit tests
    // public APLImpl(int nodeId, int destId, PrivateKey privateKey, PublicKey
    // destPublicKey,
    // MessageHandler messageHandler)
    // throws SocketException, GeneralSecurityException, IOException {
    // this(nodeId, destId, privateKey, destPublicKey);
    // this.messageHandler = messageHandler;
    // }

    // No public key needed. This is used for clients
    public APLImpl(String nodeAddress, Integer nodePort, String destAddress, Integer destPort, PrivateKey privateKey,
            DatagramSocket socket, MessageHandler messageHandler)
            throws GeneralSecurityException, IOException {
        this(nodeAddress, nodePort, destAddress, destPort, privateKey, null, socket, messageHandler);
    }

    public void send(Serializable obj) {
        byte[] messageBytes = null;
        try {
            messageBytes = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            Logger.ERROR("Failed to serialize message: " + e.getMessage(), e);
            return;
        }
        send(messageBytes);
    }

    public void send(byte[] message) {
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum.getAndIncrement();
        // print the seqnum
        Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());

        try {
            byte[] hmac = generateHMAC(message, seqNum, secretKey);

            DataMessage dataMsg = new DataMessage(message, seqNum, hmac);

            // Store the message in the pending list
            pendingMessages.put(seqNum, dataMsg);

            Logger.LOG(destPort + ") Sending message: " + seqNum);
            sendUdpPacket(dataMsg.serialize());
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send message: " + e.getMessage(), e);
        }
    }

    // This method is used to test the integrity of the message
    public void sendAndTamper(Serializable obj) {
        byte[] message = null;
        try {
            message = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            Logger.ERROR("Failed to serialize message: " + e.getMessage(), e);
            return;
        }
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum.getAndIncrement();
        // print the seqnum
        Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());

        try {
            byte[] hmac = generateHMAC(message, seqNum, secretKey);

            // Tamper the message
            byte[] tampered = "tampered".getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(message.length + tampered.length);
            buffer.put(message);
            buffer.put(tampered);
            byte[] tamperedMessage = buffer.array();

            DataMessage dataMsg = new DataMessage(tamperedMessage, seqNum, hmac);

            // Store the message in the pending list
            pendingMessages.put(seqNum, dataMsg);

            Logger.LOG("Sending message: " + seqNum);
            sendUdpPacket(dataMsg.serialize());
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send message: " + e.getMessage(), e);
        }
    }

    public void sendWithTimeout(Serializable obj, int timeout) {
        byte[] messageBytes = null;
        try {
            messageBytes = SerializationUtils.serializeObject(obj);
        } catch (IOException e) {
            Logger.ERROR("Failed to serialize message: " + e.getMessage(), e);
            return;
        }
        sendWithTimeout(messageBytes, timeout);
    }

    public void sendWithTimeout(byte[] message, int timeout) {
        SecretKey secretKey = getOrGenerateSecretKey();
        long seqNum = nextSeqNum.getAndIncrement();
        // print the seqnum
        Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());

        try {
            byte[] hmac = generateHMAC(message, seqNum, secretKey);

            DataMessage dataMsg = new DataMessage(message, seqNum, hmac, timeout);

            // Store the message in the pending list
            pendingMessages.put(seqNum, dataMsg);

            Logger.LOG(destPort + ") Sending message: " + seqNum);
            sendUdpPacket(dataMsg.serialize());
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send message: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    public boolean handleData(int senderId, byte[] data) {
        try {
            processReceivedPacket(senderId, data);
            return true;
        } catch (Exception e) {
            Logger.ERROR("Failed to handle data: ", e);
            return false;
        }
    }

    protected void processReceivedPacket(int senderId, byte[] data) {
        Message message = Message.deserialize(data);
        if (message == null)
            return;

        Logger.LOG(nodePort + ") Received message: " + message.toStringExtended());

        if (!lastReceivedSeqNum.compareAndSet(message.getSeqNum() - 1, message.getSeqNum())) {

            Logger.LOG(
                    "Received message was out-of-order : Message seqnum: " + message.getSeqNum() + ". From: " + senderId
                            + ". Last received: " + lastReceivedSeqNum.get());

            return;
        }

        switch (message.getType()) {
            case Message.DATA_MESSAGE_TYPE:
                DataMessage dataMessage = (DataMessage) message;
                handleData(senderId, dataMessage);
                break;
            case Message.ACK_MESSAGE_TYPE:
                AckMessage ackMessage = (AckMessage) message;
                handleACK(ackMessage);
                break;
            case Message.KEY_MESSAGE_TYPE:
                KeyMessage keyMessage = (KeyMessage) message;
                handleKey(keyMessage);
                break;
            default:
                Logger.ERROR("Unknown message type: " + message.getType());
        }
    }

    private void handleKey(KeyMessage keyMessage) {
        Logger.LOG("Received key message");
        SecretKey secretKey = null;
        try {
            secretKey = CryptoUtils.decryptSymmetricKey(keyMessage.getContent(), privateKey);
        } catch (Exception e) {
            Logger.ERROR("Failed to decrypt symmetric key: " + e.getMessage(), e);
            return;
        }

        this.secretKey = secretKey;

        Logger.LOG("Received key: " + secretKey);

        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(keyMessage.getSeqNum());
        } catch (Exception e) {
            Logger.LOG("Failed to send acknowledgment: " + e.getMessage());
        }

        // Check for duplicates
        if (isDuplicate(keyMessage.getSeqNum())) {
            return;
        }
    }

    private void handleACK(AckMessage ackMessage) {
        byte[] content = ackMessage.getContent();
        long seqNum = ackMessage.getSeqNum();
        byte[] hmac = ackMessage.getMac();

        if (!verifyHMAC(content, seqNum, hmac)) {
            Logger.LOG("Authentication failed for ACK: " + seqNum);
            return;
        }

        // Remove the message from the pending list
        if (!pendingMessages.containsKey(seqNum)) {
            Logger.LOG("Received ACK for unsent message: " + seqNum);
            return;
        }

        Logger.LOG("Received ACK for message: " + seqNum);
        // Logger.LOG("attempting to remove message: " + seqNum + " from pending
        // list\n...");
        // print messages in pending list
        // Logger.LOG("Pending messages before:");
        // for (Long key : pendingMessages.keySet()) {
        // Logger.LOG("Pending message: " + key);
        // }
        pendingMessages.remove(seqNum);

        // print messages in pending list
        // Logger.LOG("...\nPending messages after:");
        // for (Long key : pendingMessages.keySet()) {
        // Logger.LOG("Pending message: " + key);
        // }
        // Logger.LOG("...");
    }

    private void handleData(Integer senderId, DataMessage dataMessage) {
        byte[] content = dataMessage.getContent();
        long seqNum = dataMessage.getSeqNum();
        byte[] hmac = dataMessage.getMac();

        if (!verifyHMAC(content, seqNum, hmac)) {
            Logger.LOG("Authentication failed for message");
            return;
        }

        // Send authenticated acknowledgment
        try {
            sendAuthenticatedAcknowledgment(seqNum);
        } catch (Exception e) {
            Logger.ERROR("Failed to send acknowledgment: " + e.getMessage(), e);
        }

        // Check for duplicates
        if (isDuplicate(seqNum)) {
            return;
        }

        try {

            // Deliver to application
            if (messageHandler != null) {
                messageHandler.onMessage(senderId, content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.", new Exception());
            }
        } catch (Exception e) {
            Logger.ERROR("Failed to deserialize message content: ", e);
            e.printStackTrace();

            // Still deliver the raw bytes if deserialization fails
            if (messageHandler != null) {
                messageHandler.onMessage(senderId, content);
            } else {
                Logger.ERROR("No message handler set. Failed to deliver message.", new Exception());
            }
        }
    }

    private boolean isDuplicate(long seqNum) {
        return receivedMessages.putIfAbsent(seqNum, Boolean.TRUE) != null;
    }

    private void sendAuthenticatedAcknowledgment(long dataSeqNum) throws Exception {
        try {
            SecretKey secretKey = getSecretKey();
            long seqNum = nextSeqNum.getAndIncrement();
            // create the content from the dataSeqNum
            byte[] content = ByteBuffer.allocate(Long.BYTES).putLong(dataSeqNum).array();

            byte[] hmac = generateHMAC(content, seqNum, secretKey);

            AckMessage ackMessage = new AckMessage(content, seqNum, hmac);

            Logger.LOG("Sending ACK for message: " + seqNum);
            sendUdpPacket(ackMessage.serialize());
        } catch (Exception e) {
            Logger.ERROR("Failed to sign and send ACK", e);
            throw e;
        }
    }

    private void sendUdpPacket(byte[] data) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, destAddress, destPort);
            socket.send(packet);
        } catch (Exception e) {
            Logger.ERROR("Failed to send UDP packet", e);
        }
    }

    private void startRetransmissionScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            Logger.LOG("Pending messages before retransmission 1:");
            for (Long key : pendingMessages.keySet()) {
                Logger.LOG("Pending message: " + key);
            }
            
            Set<Long> timedOutSeqNums = new HashSet<>();
            
            //Check timeouts
            for (Map.Entry<Long, Message> entry : pendingMessages.entrySet()) {
                Long seqNum = entry.getKey();
                Message message = entry.getValue();
                
                if (message.getCounter() >= message.getTimeout()) {
                    Logger.LOG("Message timed out: " + seqNum);
                    timedOutSeqNums.add(seqNum);
                }
            }
            
            //handle timeouts
            if (!timedOutSeqNums.isEmpty()) {
                for (Long seqNum : timedOutSeqNums) {
                    pendingMessages.remove(seqNum);
                }
                
                Logger.LOG("Decreasing every message's seqnum");
                
                Long lowestTimedOutSeqNum = Collections.min(timedOutSeqNums);
                List<Message> messagesToUpdate = new ArrayList<>();
                
                for (Message msg : pendingMessages.values()) {
                    if (msg.getSeqNum() > lowestTimedOutSeqNum) {
                        messagesToUpdate.add(msg);
                    }
                }
                
                messagesToUpdate.sort((m1, m2) -> Long.compare(m2.getSeqNum(), m1.getSeqNum()));
                
                // Update sequence numbers
                for (Message msg : messagesToUpdate) {
                    pendingMessages.remove(msg.getSeqNum());
                    msg.setSeqNum(msg.getSeqNum() - timedOutSeqNums.size());
                    pendingMessages.put(msg.getSeqNum(), msg);
                    //TODO: reset the timeout of the message?
                }
                
                nextSeqNum.addAndGet(-timedOutSeqNums.size());
                Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());
                
                return;
            }
            
            for (Map.Entry<Long, Message> entry : pendingMessages.entrySet()) {
                Long seqNum = entry.getKey();
                Message message = entry.getValue();
                
                if (message.getCounter() >= message.getCooldown()) {
                    Logger.LOG("Retransmitting message with seqNum: " + seqNum +
                            "\nWaited cooldown: " + message.getCooldown() * 0.05 + "s");
                    try {
                        sendUdpPacket(message.serialize());
                    } catch (Exception e) {
                        Logger.ERROR("Failed to retransmit message: " + e.getMessage(), e);
                    }
                    message.setCounter(1);
                    message.doubleCooldown(); // Exponential backoff
                } else {
                    message.incrementCounter();
                }
            }

            Logger.LOG("Pending messages before retransmission 2:");
            for (Long key : pendingMessages.keySet()) {
                Logger.LOG("Pending message: " + key);
            }
        }, Config.RETRANSMISSION_TIME, Config.RETRANSMISSION_TIME, TimeUnit.MILLISECONDS);
    }

    private boolean verifyHMAC(byte[] content, long seqNum, byte[] hmac) {
        try {
            SecretKey secretKey = getSecretKey();

            String data = nodeAddress.toString() + String.valueOf(nodePort) +
                    Arrays.toString(content) + Long.toString(seqNum);

            return CryptoUtils.verifyHMAC(data, secretKey, hmac);
        } catch (Exception e) {
            Logger.ERROR("Failed to verify HMAC: " + e.getMessage(), e);
            return false;
        }
    }

    private byte[] generateHMAC(byte[] content, long seqNum, SecretKey secretKey) {
        try {

            String data = destAddress.toString() + String.valueOf(destPort) +
                    Arrays.toString(content) + Long.toString(seqNum);

            return CryptoUtils.generateHMAC(data, secretKey);
        } catch (Exception e) {
            Logger.ERROR("Failed to generate HMAC: " + e.getMessage(), e);
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

    private SecretKey getSecretKey() {
        return secretKey;
    }

    private SecretKey generateAndShareSecretKey() {
        try {
            SecretKey secretKey = CryptoUtils.generateSymmetricKey();

            byte[] encryptedKey = CryptoUtils.encryptSymmetricKey(secretKey, destPublicKey);

            long seqNum = nextSeqNum.getAndIncrement();
            // print the seqnum
            Logger.LOG("Port: " + this.destPort + " Next seqnum: " + nextSeqNum.get());

            KeyMessage keyMessage = new KeyMessage(encryptedKey, seqNum);
            pendingMessages.put(seqNum, keyMessage);

            Logger.LOG("Sending key: " + secretKey);
            sendUdpPacket(keyMessage.serialize());

            return secretKey;
        } catch (Exception e) {
            Logger.ERROR("Failed to generate and share secret key: " + e.getMessage(), e);
            return null;
        }
    }

    public void close() {
        scheduler.shutdown();
    }
}