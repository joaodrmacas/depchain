// package pt.tecnico.ulisboa;

// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertTrue;
// import static org.junit.Assert.fail;

// import java.security.KeyPair;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicInteger;

// import javax.crypto.SecretKey;

// import org.junit.Test;

// import pt.tecnico.ulisboa.network.APL;
// import pt.tecnico.ulisboa.network.APLImpl;
// import pt.tecnico.ulisboa.network.MessageHandler;
// import pt.tecnico.ulisboa.network.message.DataMessage;
// import pt.tecnico.ulisboa.network.message.KeyMessage;
// import pt.tecnico.ulisboa.utils.CryptoUtils;


// public class AuthenticatedPerfectLinkTests {

//     @Test
//     public void testBasicDelivery() {
//         KeyPair senderKeys = CryptoUtils.generateKeyPair(2048);
//         KeyPair receiverKeys = CryptoUtils.generateKeyPair(2048);
//         int senderId = 1;
//         int receiverId = 2;
//         String message = "APPEND A";

//         try {
//             APL sender = new APLImpl(senderId,receiverId,senderKeys.getPrivate(), receiverKeys.getPublic());
//             APL receiver = new APLImpl(receiverId,senderId,receiverKeys.getPrivate(), senderKeys.getPublic());

//             receiver.setMessageHandler((id, receivedMessage) -> {
//                 assertEquals(senderId, id);
//                 assertEquals(message, new String(receivedMessage));
//             });

//             sender.send(message.getBytes());

//         } catch (Exception e) {
//             fail();
//         }
//     }

//     @Test 
//     public void testMessageResend() {
//         KeyPair senderKeys = CryptoUtils.generateKeyPair(2048);
//         KeyPair receiverKeys = CryptoUtils.generateKeyPair(2048);
//         int senderId = 3;
//         int receiverId = 4;
//         String message = "APPEND B";
        
//         try {
//             CountDownLatch latch = new CountDownLatch(1);
            
//             // Create sender but don't create receiver yet
//             APL sender = new APLImpl(senderId, receiverId, senderKeys.getPrivate(), receiverKeys.getPublic());
            
//             // Send message while receiver is not active
//             sender.send(message.getBytes());
            
//             // Sleep to allow for some retransmission attempts
//             Thread.sleep(2000);
            
//             // Now create the receiver and set up message handler
//             MessageHandler handler = (id, receivedMessage) -> {
//                 assertEquals(senderId, id);
//                 assertEquals(message, new String(receivedMessage));
//                 latch.countDown();
//             };

//             APL receiver = new APLImpl(receiverId, senderId, receiverKeys.getPrivate(), senderKeys.getPublic(), handler);
            
            
//             // Wait for message to be delivered via retransmission
//             assertTrue("Message was not delivered after retransmission", latch.await(10, TimeUnit.SECONDS));
            
//         } catch (Exception e) {
//             fail("Exception during test: " + e.getMessage());
//         }
//     }

//     @Test
//     public void testDuplicateDetection() {
//         KeyPair senderKeys = CryptoUtils.generateKeyPair(2048);
//         KeyPair receiverKeys = CryptoUtils.generateKeyPair(2048);
//         int senderId = 5;
//         int receiverId = 6;
//         String message = "APPEND C";
        
//         try {
//             AtomicInteger deliveryCount = new AtomicInteger(0);
//             CountDownLatch latch = new CountDownLatch(1);
            
//             APL sender = new APLImpl(senderId, receiverId, senderKeys.getPrivate(), receiverKeys.getPublic());
            
//             //send a message
//             sender.send(message.getBytes());
            
//             // Sleep to give time for retransmission
//             Thread.sleep(2000);

//             // Create receiver and set up message handler
//             MessageHandler handler = (id, receivedMessage) -> {
//                 assertEquals(senderId, id);
//                 assertEquals(message, new String(receivedMessage));
//                 deliveryCount.incrementAndGet();
//                 latch.countDown();
//             };
//             APL receiver = new APLImpl(receiverId, senderId, receiverKeys.getPrivate(), senderKeys.getPublic(), handler);
            
//             // Verify message was delivered exactly once
//             assertEquals("Message should be delivered exactly once", 1, deliveryCount.get());
            
//         } catch (Exception e) {
//             fail("Exception during test: " + e.getMessage());
//         }
//     }

//     @Test 
//     public void testHMACAuthentication() {
//         KeyPair senderKeys = CryptoUtils.generateKeyPair(2048);
//         KeyPair receiverKeys = CryptoUtils.generateKeyPair(2048);
//         KeyPair attackerKeys = CryptoUtils.generateKeyPair(2048); // Different keys
//         int senderId = 7;
//         int receiverId = 8;
//         int attackerId = 9;
//         String message = "APPEND D";
        
//         try {
//             CompletableFuture<Boolean> legitimateDelivery = new CompletableFuture<>();
//             CompletableFuture<Boolean> attackerDelivery = new CompletableFuture<>();
            
//             // Set up legitimate communication
//             APLImpl sender = new APLImpl(senderId, receiverId, senderKeys.getPrivate(), receiverKeys.getPublic());
//             // Set message handler on receiver
//             MessageHandler handler = (id, receivedMessage) -> {
//                 if (id == senderId) {
//                     legitimateDelivery.complete(true);
//                 } else if (id == attackerId) {
//                     attackerDelivery.complete(true);
//                 }
//             };
//             APLImpl receiver = new APLImpl(receiverId, senderId, receiverKeys.getPrivate(), senderKeys.getPublic(),handler);
            
//             // Set up attacker trying to impersonate legitimate sender
//             APLImpl attacker = new APLImpl(attackerId, receiverId, attackerKeys.getPrivate(), receiverKeys.getPublic());

//             SecretKey hmacKey = CryptoUtils.generateSymmetricKey();
//             KeyMessage keyMessage = new KeyMessage(hmacKey.getEncoded(), 1);

//             //Done under the hood when sending the first message on APLinks
//             sender.sendKeyMessage(keyMessage);

//             byte[] hmac = CryptoUtils.generateHMAC(message, hmacKey);
//             DataMessage dataMessage = new DataMessage(message.getBytes(), 2, hmac);
            
//             // Simulate message interception and message change
//             DataMessage changedMessage = new DataMessage("APPEND ATTACK".getBytes(), 2, hmac);
//             attacker.sendDataMessage(dataMessage);

//             sender.sendDataMessage(changedMessage);
            
//             // Check that legitimate message is delivered
//             assertTrue("Legitimate message should be delivered", legitimateDelivery.get(5, TimeUnit.SECONDS));
            
//             // The attacker message should never be delivered with the sender ID
//             // This is enforced by the APL's destination checking logic
//             try {
//                 attackerDelivery.get(3, TimeUnit.SECONDS);
//                 fail("Attacker message should not be accepted as legitimate");
//             } catch (java.util.concurrent.TimeoutException e) {
//                 // Expected timeout - attacker message was not delivered
//             }
            
//         } catch (Exception e) {
//             fail("Exception during test: " + e.getMessage());
//         }
//     }
// }