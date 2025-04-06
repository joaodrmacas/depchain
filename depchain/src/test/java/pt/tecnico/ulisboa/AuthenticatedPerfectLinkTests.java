package pt.tecnico.ulisboa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pt.tecnico.ulisboa.network.AplManager;
import pt.tecnico.ulisboa.network.MessageHandler;
import pt.tecnico.ulisboa.network.ServerAplManager;
import pt.tecnico.ulisboa.utils.CryptoUtils;
import pt.tecnico.ulisboa.utils.SerializationUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

public class AuthenticatedPerfectLinkTests {

    private static final int SOURCE_PORT = 8079;
    private static final int DESTINATION_PORT = 8078;
    private static final int ATTACKER_PORT = 8077;
    private static final String ADDR = "127.0.0.1";
    private static final int TIMEOUT_MS = 1000;

    private AplManager senderManager;
    private AplManager receiverManager;
    private KeyPair senderKeys;
    private KeyPair receiverKeys;
    private KeyPair maliciousKeys;
    private final int senderId = 1;
    private final int receiverId = 2;

    @Before
    public void setup() {
        // Generate fresh key pairs for each test
        senderKeys = CryptoUtils.generateKeyPair(2048);
        receiverKeys = CryptoUtils.generateKeyPair(2048);
        maliciousKeys = CryptoUtils.generateKeyPair(2048);
    }

    @After
    public void cleanup() {
        // Ensure resources are properly closed after each test
        if (senderManager != null) {
            senderManager.close();
            senderManager = null;
        }
        if (receiverManager != null) {
            receiverManager.close();
            receiverManager = null;
        }
    }

    /**
     * Creates and initializes sender and receiver managers for testing
     * and starts listening for messages on both managers.
     * 
     * @param messageHandler The callback handler for received messages or null if
     *                       not needed
     */
    private void initManagers(MessageHandler messageHandler) throws Exception {
        // Create sender manager
        senderManager = new ServerAplManager(ADDR, SOURCE_PORT, senderKeys.getPrivate());
        senderManager.createAPL(receiverId, ADDR, DESTINATION_PORT, receiverKeys.getPublic(), null);
        senderManager.startListening();

        // Create receiver manager
        receiverManager = new ServerAplManager(ADDR, DESTINATION_PORT, receiverKeys.getPrivate());
        receiverManager.createAPL(senderId, ADDR, SOURCE_PORT, senderKeys.getPublic(), messageHandler);
        receiverManager.startListening();
    }

    @Test
    public void testBasicMessageSendingAndReceiving() {
        final String message = "CLIENT A";
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final AtomicReference<String> receivedMessage = new AtomicReference<>();
        final AtomicReference<Integer> receivedId = new AtomicReference<>();
        final MessageHandler messageHandler = (id, msgBytes) -> {
            try {
                receivedId.set(id);
                receivedMessage.set(SerializationUtils.deserializeObject(msgBytes));
                messageLatch.countDown();
            } catch (ClassNotFoundException | IOException e) {
                fail("Failed to deserialize message: " + e.getMessage());
            }
        };

        try {
            initManagers(messageHandler);

            senderManager.send(receiverId, message);

            // Wait for message to be received (we use a timeout to ensure that it has
            // enough time to arrive)
            boolean received = messageLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue("Message reception timed out", received);

            assertEquals("Sender ID should match", senderId,
                    receivedId.get().intValue());
            assertEquals("Message content should match", message, receivedMessage.get());

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testMessagesUniqueAndInOrder() {
        final String[] messages = { "Message 1", "Message 2", "Message 3", "Message4", "Message 5", "Message 6",
                "Message 7", "Message 8", "Message 9", "Message 10" };
        final CountDownLatch messageLatch = new CountDownLatch(messages.length);
        final List<String> receivedMessages = new ArrayList<>();
        final MessageHandler messageHandler = (id, msgBytes) -> {
            try {
                assertEquals("Sender ID should match", senderId, id);
                receivedMessages.add(SerializationUtils.deserializeObject(msgBytes));
                messageLatch.countDown();
            } catch (ClassNotFoundException | IOException e) {
                fail("Failed to deserialize message: " + e.getMessage());
            }
        };

        try {
            initManagers(messageHandler);

            // Send multiple messages
            for (String message : messages) {
                senderManager.send(receiverId, message);
            }

            // Wait for all messages to be received with timeout
            boolean allReceived = messageLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue("Not all messages were received within timeout", allReceived);

            // Verify count and uniqueness
            assertEquals("Should receive all messages", messages.length,
                    receivedMessages.size());
            assertEquals("Should receive unique messages", messages.length, new HashSet<>(receivedMessages).size());

            // Verify order
            for (int i = 0; i < messages.length; i++) {
                assertEquals("Messages should be received in order", messages[i],
                        receivedMessages.get(i));
            }

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testMessageDeliveryWithNetworkPartition() {
        final String message = "Partition Test";
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final AtomicReference<String> receivedMessage = new AtomicReference<>();
        final MessageHandler messageHandler = (id, msgBytes) -> {
            try {
                receivedMessage.set(SerializationUtils.deserializeObject(msgBytes));
                messageLatch.countDown();
            } catch (ClassNotFoundException | IOException e) {
                fail("Failed to deserialize message: " + e.getMessage());
            }
        };

        try {
            initManagers(messageHandler);

            // Simulate network partition by making the receiver drop all packets
            receiverManager.setListening(false);
            senderManager.send(receiverId, message);

            // Verify message was not received (wait a bit to be sure)
            Thread.sleep(500);
            assertEquals("No message should be received during partition", 1,
                    messageLatch.getCount());

            // Restore connection by restarting listening on the same manager
            receiverManager.setListening(true);

            // Wait for message to be received
            boolean allReceived = messageLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue("Message not received after network restored", allReceived);
            assertEquals("Message content should match", message, receivedMessage.get());

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testSameMessageNotConsideredDuplicate() {
        final String message = "I can send the same message multiple times. Itdoesn't mean it's a duplicate, just the same message content.";
        final int sendingTimes = 5;
        final CountDownLatch messageLatch = new CountDownLatch(sendingTimes);
        final List<String> receivedMessages = new ArrayList<>();
        final MessageHandler messageHandler = (id, msgBytes) -> {
            // Ensure the correct sender and message and increment the delivery count
            assertEquals(senderId, id);
            try {
                receivedMessages.add(SerializationUtils.deserializeObject(msgBytes));
                messageLatch.countDown();
            } catch (ClassNotFoundException | IOException e) {
                fail("Failed to deserialize message: " + e.getMessage());
            }
        };

        try {
            initManagers(messageHandler);

            // Send the same CONTENT multiple times
            for (int i = 0; i < 5; i++) {
                senderManager.send(receiverId, message);
            }

            // Wait for receipt
            boolean received = messageLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue("No message was received", received);

            // Wait a bit more to ensure delivery
            Thread.sleep(500);

            assertEquals("All instances of the message shoud be received", sendingTimes,
                    receivedMessages.size());
            for (String receivedMessage : receivedMessages) {
                assertEquals("Message content should match", message, receivedMessage);
            }

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testAttackerImpersonatingServer() {
        final String legitimateMessage = "This is a legitimate message";
        final String maliciousMessage = "This is a malicious message";
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final List<String> receivedMessages = new ArrayList<>();
        final CountDownLatch maliciousLatch = new CountDownLatch(1);

        final MessageHandler messageHandler = (id, msgBytes) -> {
            try {
                String message = SerializationUtils.deserializeObject(msgBytes);
                receivedMessages.add(message);
                messageLatch.countDown();
            } catch (ClassNotFoundException | IOException e) {
                fail("Failed to deserialize message: " + e.getMessage());
            }
        };

        try {
            // Set up legitimate communication channel with our real keys
            initManagers(messageHandler);

            // Create a malicious manager with its own keys
            ServerAplManager maliciousManager = new ServerAplManager(ADDR, ATTACKER_PORT,
                    maliciousKeys.getPrivate());
            maliciousManager.createAPL(receiverId, ADDR, DESTINATION_PORT,
                    receiverKeys.getPublic(), null);

            // First, send a legitimate message from the authorized sender
            senderManager.send(receiverId, legitimateMessage);

            // Wait for the legitimate message
            boolean received = messageLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue("Legitimate message was not received", received);
            assertEquals("Should receive exactly one message", 1,
                    receivedMessages.size());
            assertEquals("Message content should match", legitimateMessage,
                    receivedMessages.get(0));

            // Reset for the malicious message test
            receivedMessages.clear();

            // Send a message from the malicious sender
            maliciousManager.send(receiverId, maliciousMessage);

            // Wait briefly to see if the message is delivered
            boolean maliciousReceived = maliciousLatch.await(500, TimeUnit.MILLISECONDS);
            assertFalse("Malicious message should not be received", maliciousReceived);
            assertEquals("No messages should be received from malicious sender", 0,
                    receivedMessages.size());

            // Clean up
            maliciousManager.close();

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    // // FIXME: This test is crashing the JVM
    @Test
    public void testHmacVerification() { // NOTE: For this test, we are tampering
                                         // with the message directly after its
                                         // Hmac is computed because it is hard to spoof the ip and port of the
                                         // sender.
                                         // This way the HMAC verification should fail since it is as if the message
                                         // was
                                         // tampered with by an attacker in the network.
        final String message = "This is a message with HMAC verification";
        final CountDownLatch messageLatch = new CountDownLatch(1);
        final AtomicReference<String> receivedMessage = new AtomicReference<>();
        final MessageHandler messageHandler = (id, msgBytes) -> {
            try {
                receivedMessage
                        .set(SerializationUtils.deserializeObject(msgBytes));
                messageLatch.countDown();
            } catch (ClassNotFoundException | IOException e) {
                fail("Failed to deserialize message: " + e.getMessage());
            }

        };

        try {
            initManagers(messageHandler);

            // Send the message
            senderManager.sendAndTamper(receiverId, message);

            // Wait to ensure the message would have been received
            boolean received = messageLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // message should not be received since the hmac verification should fail
            assertFalse("Message should not be received", received);

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testMessageDeliveryAfterTimeout() {
        final String timeoutMessage = "This message will time out";
        final String followupMessage = "This message should still be delivered";
        final CountDownLatch followupLatch = new CountDownLatch(1);
        final AtomicReference<String> receivedMessage = new AtomicReference<>();

        final MessageHandler messageHandler = (id, msgBytes) -> {
            try {
                String message = SerializationUtils.deserializeObject(msgBytes);
                // print the message that was received
                Logger.LOG("Received timeout message: " + message);
                if (message.equals(followupMessage)) {
                    receivedMessage.set(message);
                    followupLatch.countDown();
                } else if (message.equals(timeoutMessage)) {
                    // This should not happen since the receiver is not listening yet
                    fail("Received the message that was supposed to time out");
                }
            } catch (ClassNotFoundException | IOException e) {
                fail("Failed to deserialize message: " + e.getMessage());
            }
        };

        try {
            initManagers(messageHandler);
            // Set the receiver to drop packets to simulate network failure
            receiverManager.setListening(false);

            // Send 2 messages where the first timesout because the receiver is not
            // listening yet
            final int shortTimeout = 0;
            senderManager.sendWithTimeout(receiverId, timeoutMessage, shortTimeout);
            senderManager.send(receiverId, followupMessage);
            // print if it is listening
            Logger.LOG("Receiver is listening: " + receiverManager.isListening());
            Thread.sleep(TIMEOUT_MS);

            // Restore the receiver to start listening again
            receiverManager.setListening(true);

            // Wait for the follow-up message to be received
            boolean received = followupLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertTrue("Follow-up message was not received after timeout", received);
            assertEquals("Message content should match", followupMessage, receivedMessage.get());

        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }

    // FIXME: Este teste falha. por mim apaga-se ja temos muitos testes. nem sei se
    // a culpa é do teste estar mal feito ou do codigo estar mal nem sei quem fez
    // este teste
    @Test
    public void testFourMessageExchangeLoop() {
        final int SERVER1_ID = 1;
        final int SERVER2_ID = 2;
        final int SERVER3_ID = 3;
        final int SERVER4_ID = 4;

        final int SERVER1_PORT = 8081;
        final int SERVER2_PORT = 8082;
        final int SERVER3_PORT = 8083;
        final int SERVER4_PORT = 8084;

        final KeyPair server1Keys = CryptoUtils.generateKeyPair(2048);
        final KeyPair server2Keys = CryptoUtils.generateKeyPair(2048);
        final KeyPair server3Keys = CryptoUtils.generateKeyPair(2048);
        final KeyPair server4Keys = CryptoUtils.generateKeyPair(2048);

        // Message to be sent
        final String messageTemplate = "Message from Server %d to Server %d";
        final int MESSAGE_COUNT = 1;

        // Synchronization mechanisms
        final CountDownLatch messageLatch = new CountDownLatch(4 * 3 *
                MESSAGE_COUNT);
        final List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

        // Managers for each server
        final ServerAplManager[] servers = new ServerAplManager[4];
        final MessageHandler[] handlers = new MessageHandler[12];

        try {
            // Create message handlers for each server
            for (int i = 0; i < 4; i++) {
                final int currentServerId = i + 1;
                final KeyPair currentKeys = i == 0 ? server1Keys
                        : i == 1 ? server2Keys : i == 2 ? server3Keys : server4Keys;
                final int currentPort = i == 0 ? SERVER1_PORT
                        : i == 1 ? SERVER2_PORT : i == 2 ? SERVER3_PORT : SERVER4_PORT;

                for (int j = 0; j < 3; j++) {
                    Logger.LOG("res: " + (i * 3 + (j)));
                    handlers[i * 3 + (j)] = (id, msgBytes) -> {
                        try {
                            String message = SerializationUtils.deserializeObject(msgBytes);
                            synchronized (receivedMessages) {
                                receivedMessages.add(message);
                            }
                            messageLatch.countDown();
                        } catch (ClassNotFoundException | IOException e) {
                            fail("Failed to deserialize message: " + e.getMessage());
                        }
                    };
                }

                // Create server managers
                servers[i] = new ServerAplManager(ADDR, currentPort,
                        currentKeys.getPrivate());
            }

            // Set up APLs between all servers
            servers[0].createAPL(SERVER2_ID, ADDR, SERVER2_PORT,
                    server2Keys.getPublic(), handlers[0]);
            servers[0].createAPL(SERVER3_ID, ADDR, SERVER3_PORT,
                    server3Keys.getPublic(), handlers[1]);
            servers[0].createAPL(SERVER4_ID, ADDR, SERVER4_PORT,
                    server4Keys.getPublic(), handlers[2]);

            servers[1].createAPL(SERVER1_ID, ADDR, SERVER1_PORT,
                    server1Keys.getPublic(), handlers[3]);
            servers[1].createAPL(SERVER3_ID, ADDR, SERVER3_PORT,
                    server3Keys.getPublic(), handlers[4]);
            servers[1].createAPL(SERVER4_ID, ADDR, SERVER4_PORT,
                    server4Keys.getPublic(), handlers[5]);

            servers[2].createAPL(SERVER1_ID, ADDR, SERVER1_PORT, server1Keys.getPublic(),
                    handlers[6]);
            servers[2].createAPL(SERVER2_ID, ADDR, SERVER2_PORT, server2Keys.getPublic(),
                    handlers[7]);
            servers[2].createAPL(SERVER4_ID, ADDR, SERVER4_PORT, server4Keys.getPublic(),
                    handlers[8]);

            servers[3].createAPL(SERVER1_ID, ADDR, SERVER1_PORT, server1Keys.getPublic(),
                    handlers[9]);
            servers[3].createAPL(SERVER2_ID, ADDR, SERVER2_PORT, server2Keys.getPublic(),
                    handlers[10]);
            servers[3].createAPL(SERVER3_ID, ADDR, SERVER3_PORT, server3Keys.getPublic(),
                    handlers[11]);

            // Start listening for all servers
            for (ServerAplManager server : servers) {
                server.startListening();
            }

            // Send messages from each server to every other server
            for (int sender = 1; sender <= 4; sender++) {
                if (sender == 3 || sender == 4)
                    continue;
                for (int receiver = 1; receiver <= 4; receiver++) {
                    if (receiver == 3 || receiver == 4)
                        continue;
                    if (sender != receiver) {
                        ServerAplManager senderManager = servers[sender - 1];
                        for (int i = 0; i < MESSAGE_COUNT; i++) {
                            String message = String.format(messageTemplate, sender, receiver);
                            senderManager.send(receiver, message);
                        }
                    }
                }
            }

            // Wait for all messages to be received
            boolean allReceived = messageLatch.await(6000, TimeUnit.MILLISECONDS);

            assertTrue("Not all messages were received within timeout", allReceived);

            // Verify message counts
            assertEquals("Total messages received should match", 4 * 3 *
                    MESSAGE_COUNT, receivedMessages.size());

            // Optionally, print received messages for verification
            System.out.println("Received " + receivedMessages.size() + " messages");

        } catch (

        Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        } finally {
            // Cleanup - close all server managers
            for (ServerAplManager server : servers) {
                if (server != null) {
                    server.close();
                }
            }
        }
    }
}