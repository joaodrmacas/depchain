package pt.tecnico.ulisboa.network.layers;

import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.network.message.*;
import java.util.concurrent.*;
import java.util.*;
import java.net.*;
import java.security.*;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AuthenticatedPerfectLinkImpl implements AuthenticatedPerfectLink {
    private final DatagramSocket socket;
    private MessageHandler messageHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final ConcurrentMap<String, AuthenticatedMessage> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> receivedMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> nextSeqNum = new ConcurrentHashMap<>();
    
    private static final int BUFFER_SIZE = 4096;
    
    private final PrivateKey privateKey;
    private final ConcurrentMap<String, PublicKey> publicKeys = new ConcurrentHashMap<>();
    private final String nodeId;
    
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
            byte[] mac = createSignature(nodeId, destId, message, seqNum);
            
            DataMessage dataMsg = new DataMessage(message, port, nodeId, destId, seqNum, mac);
            String messageId = nodeId + ":" + destId + ":" + seqNum;
            dataMsg.setKey(messageId);
            
            pendingMessages.put(messageId, dataMsg);
            
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
        if (message.getType() == AckMessage.TYPE_INDICATOR) {
            AckMessage ackMessage = (AckMessage) message;
            String senderId = ackMessage.getSenderId();
            String destinationId = ackMessage.getDestinationId();
            byte[] mac = ackMessage.getMac();
            long seqNum = ackMessage.getSeqNum();
            
            // Verify the signature on the ACK
            if (!verifySignature(senderId, destinationId, ackMessage.getContent(), seqNum, mac)) {
                System.err.println("Authentication failed for ACK from " + senderId);
                return;
            }
            
            // Find the original message being acknowledged
            String messageId = destinationId + ":" + senderId + ":" + seqNum;
            pendingMessages.remove(messageId);
            return;
        }
        
        // Handle authenticated data messages from other processes
        if (message.getType() == DataMessage.TYPE_INDICATOR) {
            DataMessage dataMessage = (DataMessage) message;
            long seqNum = dataMessage.getSeqNum();
            String senderId = dataMessage.getSenderId();
            String destinationId = dataMessage.getDestinationId();
            byte[] mac = dataMessage.getMac();
            byte[] content = dataMessage.getContent();
            
            // Verify the signature (APL3: Authenticity)
            if (!verifySignature(senderId, destinationId, content, seqNum, mac)) {
                System.err.println("Authentication failed for message from " + senderId);
                return;
            }
            
            // Send authenticated acknowledgment
            try {
                sendAuthenticatedAcknowledgment(sender, senderId, seqNum);
            } catch (Exception e) {
                System.err.println("Failed to send acknowledgment: " + e.getMessage());
            }
            
            // Check for duplicates (APL2: No duplication)
            if (isDuplicate(senderId, seqNum)) {
                return;
            }
            
            // Mark as received to prevent future duplicates
            markAsReceived(senderId, seqNum);
            
            // Deliver to application (APL3: Authenticity guaranteed by signature verification)
            if (messageHandler != null) {
                messageHandler.onMessage(senderId, content);
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
    
    private void sendAuthenticatedAcknowledgment(String receiver, String senderId, long seqNum) throws Exception {
        try {
            String[] parts = receiver.split(":");
            String destIP = parts[0];
            int destPort = Integer.parseInt(parts[1]);
            
            // Empty content for ACK message
            byte[] emptyContent = "".getBytes();
            
            // Sign the ACK with our private key
            byte[] mac = createSignature(nodeId, senderId, emptyContent, seqNum);
            
            // Create authenticated ACK message
            AckMessage ackMessage = new AckMessage(destPort, nodeId, senderId, seqNum, mac);
            
            // Send it
            sendUdpPacket(destIP, destPort, ackMessage.serialize());
        } catch (Exception e) {
            System.err.println("Failed to sign and send ACK: " + e.getMessage());
            throw e;
        }
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
            for (AuthenticatedMessage message : pendingMessages.values()) {
                // Retransmit messages that haven't been acknowledged
                if (message.getCounter() >= message.getCooldown()) {
                    message.setCounter(1);
                    String destinationId = message.getDestinationId();
                    String[] parts = destinationId.split(":");
                    if (parts.length >= 2) {
                        String destination = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        sendUdpPacket(destination, port, message.serialize());
                    }
                    message.doubleCooldown(); // Exponential backoff
                } else {
                    message.incrementCounter();
                }
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a signature for the message using the format: SenderId + ReceivedId + Message + SeqNum
     */
    private byte[] createSignature(String senderId, String receiverId, byte[] message, long seqNum) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            
            // Create the byte array with the requested format: SenderId + ReceivedId + Message + SeqNum
            ByteBuffer buffer = ByteBuffer.allocate(
                senderId.getBytes().length + 
                receiverId.getBytes().length + 
                message.length + 
                8 // 8 bytes for long seqNum
            );
            
            buffer.put(senderId.getBytes());
            buffer.put(receiverId.getBytes());
            buffer.put(message);
            buffer.putLong(seqNum);
            
            signature.update(buffer.array());
            return signature.sign();
        } catch (Exception e) {
            System.err.println("Failed to create signature: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Verifies a signature for the message using the format: SenderId + ReceivedId + Message + SeqNum
     */
    private boolean verifySignature(String senderId, String receiverId, byte[] message, long seqNum, byte[] signatureBytes) {
        try {
            PublicKey publicKey = publicKeys.get(senderId);
            if (publicKey == null) {
                System.err.println("No public key found for sender: " + senderId);
                return false;
            }
            
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            
            // Create the byte array with the same format used for signing
            ByteBuffer buffer = ByteBuffer.allocate(
                senderId.getBytes().length + 
                receiverId.getBytes().length + 
                message.length + 
                8 // 8 bytes for long seqNum
            );
            
            buffer.put(senderId.getBytes());
            buffer.put(receiverId.getBytes());
            buffer.put(message);
            buffer.putLong(seqNum);
            
            verifier.update(buffer.array());
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            System.err.println("Signature verification failed: " + e.getMessage());
            return false;
        }
    }
}