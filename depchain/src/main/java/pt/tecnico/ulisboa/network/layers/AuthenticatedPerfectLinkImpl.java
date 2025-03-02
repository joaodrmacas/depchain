package pt.tecnico.ulisboa.network.layers;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.network.message.*;
import java.util.concurrent.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AuthenticatedPerfectLinkImpl implements AuthenticatedPerfectLink {
    private final DatagramSocket socket;
    private MessageHandler messageHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final ConcurrentMap<String, DataMessage> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> receivedMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> nextSeqNum = new ConcurrentHashMap<>();
    
    private static final int BUFFER_SIZE = 4096;
    
    private final PrivateKey privateKey;
    private final ConcurrentMap<String, PublicKey> publicKeys = new ConcurrentHashMap<>();
    private final String nodeId; // Unique identifier for this node
    
    public AuthenticatedPerfectLinkImpl(String destination, int port, String nodeId, PrivateKey privateKey, Map<String, PublicKey> publicKeys) throws SocketException, GeneralSecurityException, IOException {
        this.socket = new DatagramSocket(port, InetAddress.getByName(destination));
        this.nodeId = nodeId;
        
        this.privateKey = privateKey;
        this.publicKeys.putAll(publicKeys);
        
        startListening();
        startRetransmissionScheduler();
        System.out.println("AuthenticatedPerfectLink started on port: " + port + " with node ID: " + nodeId);
    }

    @Override
    public void send(String destination, int port, byte[] message) {
        String destId = destination + ":" + port;
        
        // Assign sequence number for this message
        long seqNum = nextSeqNum.getOrDefault(destId, 1L);
        nextSeqNum.put(destId, seqNum + 1);
        
        try {
            // Create signature for the message
            byte[] signature = createSignature(message, seqNum);
            
            // Create authenticated data message
            AuthenticatedDataMessage dataMsg = new AuthenticatedDataMessage(
                destination, port, message, seqNum, nodeId, signature
            );
            String messageId = destId + ":" + seqNum;
            dataMsg.setKey(messageId);
            
            // Store for potential retransmission
            pendingMessages.put(messageId, dataMsg);
            
            // Send the message
            sendUdpPacket(destination, port, dataMsg.serialize());
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
                    String sender = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    
                    processReceivedPacket(sender, data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private void processReceivedPacket(String sender, byte[] data) {
        Message message = Message.deserialize(data);
        if (message == null) return;
        
        // Handle authenticated acknowledgments for messages we sent
        if (message.getType() == AuthenticatedAckMessage.TYPE_INDICATOR) {
            AuthenticatedAckMessage ackMessage = (AuthenticatedAckMessage) message;
            String senderId = ackMessage.getSenderId();
            byte[] signature = ackMessage.getSignature();
            long seqNum = ackMessage.getSeqNum();
            
            // Verify signature on the ACK
            if (!verifyAckSignature(senderId, seqNum, signature)) {
                System.err.println("Authentication failed for ACK from " + senderId);
                return;
            }
            
            handleAcknowledgment(sender, seqNum);
            return;
        }
        
        // Handle authenticated data messages from other processes
        if (message.getType() == AuthenticatedDataMessage.TYPE_INDICATOR) {
            AuthenticatedDataMessage dataMessage = (AuthenticatedDataMessage) message;
            long seqNum = dataMessage.getSeqNum();
            String senderId = dataMessage.getSenderId();
            byte[] signature = dataMessage.getSignature();
            byte[] content = dataMessage.getContent();
            
            // Verify signature (APL3: Authenticity)
            if (!verifyDataSignature(senderId, content, seqNum, signature)) {
                System.err.println("Authentication failed for message from " + senderId);
                return;
            }
            
            // Send authenticated acknowledgment
            sendAuthenticatedAcknowledgment(sender, seqNum);
            
            // Check for duplicates (APL2: No duplication)
            if (isDuplicate(sender, seqNum)) {
                return;
            }
            
            // Mark as received to prevent future duplicates
            markAsReceived(sender, seqNum);
            
            // Deliver to application (APL3: Authenticity guaranteed by signature verification)
            if (messageHandler != null) {
                messageHandler.onMessage(sender, content);
            }
        }
    }
    
    private boolean isDuplicate(String sender, long seqNum) {
        Set<Long> received = receivedMessages.computeIfAbsent(sender, k -> ConcurrentHashMap.newKeySet());
        return !received.add(seqNum);
    }
    
    private void markAsReceived(String sender, long seqNum) {
        receivedMessages.computeIfAbsent(sender, k -> ConcurrentHashMap.newKeySet()).add(seqNum);
    }
    
    private void sendAuthenticatedAcknowledgment(String receiver, long seqNum) {
        try {
            String[] parts = receiver.split(":");
            String destIP = parts[0];
            int destPort = Integer.parseInt(parts[1]);
            
            // Sign the ACK
            byte[] signature = createAckSignature(seqNum);
            
            // Create authenticated ACK message
            AuthenticatedAckMessage ackMessage = new AuthenticatedAckMessage(seqNum, nodeId, signature);
            
            // Send it
            sendUdpPacket(destIP, destPort, ackMessage.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send ACK: " + e.getMessage());
        }
    }
    
    private void handleAcknowledgment(String sender, long seqNum) {
        String messageId = sender + ":" + seqNum;
        pendingMessages.remove(messageId);
    }
    
    private void sendUdpPacket(String destination, int port, byte[] data) {
        try {
            InetAddress address = InetAddress.getByName(destination);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void startRetransmissionScheduler() {
        // Implement APL1: Reliable delivery through retransmission
        scheduler.scheduleAtFixedRate(() -> {
            for (DataMessage message : pendingMessages.values()) {
                // Retransmit messages that haven't been acknowledged
                if (message.getCounter() >= message.getCooldown()) {
                    message.setCounter(1);
                    sendUdpPacket(message.getDestination(), message.getPort(), message.serialize());
                    message.doubleCooldown(); // Exponential backoff
                } else {
                    message.incrementCounter();
                }
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }
    
    
    private byte[] createDataSignature(byte[] message, long seqNum) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        
        // Include sequence number in the signature to prevent replay attacks
        ByteBuffer buffer = ByteBuffer.allocate(8 + message.length);
        buffer.putLong(seqNum);
        buffer.put(message);
        
        signature.update(buffer.array());
        return signature.sign();
    }
    
    private byte[] createAckSignature(long seqNum) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        
        // Sign just the sequence number for ACKs
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(seqNum);
        
        signature.update(buffer.array());
        return signature.sign();
    }
    
    private boolean verifyDataSignature(String senderId, byte[] message, long seqNum, byte[] signature) {
        try {
            PublicKey publicKey = publicKeys.get(senderId);
            if (publicKey == null) {
                System.err.println("No public key found for sender: " + senderId);
                return false;
            }
            
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            
            // Include sequence number in the verification
            ByteBuffer buffer = ByteBuffer.allocate(8 + message.length);
            buffer.putLong(seqNum);
            buffer.put(message);
            
            verifier.update(buffer.array());
            return verifier.verify(signature);
        } catch (Exception e) {
            System.err.println("Data signature verification failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean verifyAckSignature(String senderId, long seqNum, byte[] signature) {
        try {
            PublicKey publicKey = publicKeys.get(senderId);
            if (publicKey == null) {
                System.err.println("No public key found for sender: " + senderId);
                return false;
            }
            
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            
            // Verify just the sequence number for ACKs
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(seqNum);
            
            verifier.update(buffer.array());
            return verifier.verify(signature);
        } catch (Exception e) {
            System.err.println("ACK signature verification failed: " + e.getMessage());
            return false;
        }
    }
    
    private byte[] createSignature(byte[] message, long seqNum) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        return createDataSignature(message, seqNum);
    }
}