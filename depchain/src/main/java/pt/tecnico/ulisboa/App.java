package pt.tecnico.ulisboa;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.ulisboa.network.layers.AuthenticatedPerfectLink;
import pt.tecnico.ulisboa.network.layers.AuthenticatedPerfectLinkImpl;

public class App {

    public static void main(String[] args) {
        final int N = 2;
        final String IP = "localhost";

        try {
            KeyPair keyPair1 = generateKeyPair();
            KeyPair keyPair2 = generateKeyPair();

            Map<String, PublicKey> processIdToPublicKey = new HashMap<>();
            String process1Id = IP + ":8080";
            String process2Id = IP + ":8081";
            processIdToPublicKey.put(process1Id, keyPair1.getPublic());
            processIdToPublicKey.put(process2Id, keyPair2.getPublic());

            AuthenticatedPerfectLink process1 = new AuthenticatedPerfectLinkImpl(
                IP, 8080, process1Id, keyPair1.getPrivate(), processIdToPublicKey
            );
            AuthenticatedPerfectLink process2 = new AuthenticatedPerfectLinkImpl(
                IP, 8081, process2Id, keyPair2.getPrivate(), processIdToPublicKey
            );

            process2.setMessageHandler((sender, data) -> {
                System.out.println("Process " + process2Id + " received: " + new String(data));
            });

            System.out.println("Process 1 sending message to Process 2...");
            process1.send(IP, 8081, "Hello".getBytes());
            
            Thread.sleep(2000);
            
        } catch (Exception e) {
            System.out.println("Error in main execution");
            e.printStackTrace();
        }
    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
}