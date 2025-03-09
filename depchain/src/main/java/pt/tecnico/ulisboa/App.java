package pt.tecnico.ulisboa;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pt.tecnico.ulisboa.network.AuthenticatedPerfectLink;
import pt.tecnico.ulisboa.network.AuthenticatedPerfectLinkImpl;
import pt.tecnico.ulisboa.utils.Logger;


//Pode se apagar este lixo né - Massas

public class App {

    public static void main(String[] args) {
        try {
            // List<KeyPair> keyPairs = generateKeyPairs(N);
            // Logger.LOG("Generated " + N + " key pairs successfully");

            // Map<String, PublicKey> memberIdToPublicKey = createMemberIdToPublicKeyMap(IP, N, keyPairs);

            // startMembers(N, IP, keyPairs, memberIdToPublicKey);

        } catch (Exception e) {
            Logger.LOG("Error in main execution");
            e.printStackTrace();
        }
    }

    private static List<KeyPair> generateKeyPairs(int n) throws NoSuchAlgorithmException {
        List<KeyPair> keyPairs = new ArrayList<>();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);

        for (int i = 0; i < n; i++) {
            KeyPair keyPair = keyGen.generateKeyPair();
            keyPairs.add(keyPair);
        }

        return keyPairs;
    }

    private static Map<String, PublicKey> createMemberIdToPublicKeyMap(String ip, int n, List<KeyPair> keyPairs) {
        Map<String, PublicKey> memberIdToPublicKey = new HashMap<>();

        for (int i = 0; i < n; i++) {
            int port = 8080 + i;
            String memberId = ip + ":" + port;
            memberIdToPublicKey.put(memberId, keyPairs.get(i).getPublic());
        }

        return memberIdToPublicKey;
    }

    private static void startMembers(int n, String ip, List<KeyPair> keyPairs,
            Map<String, PublicKey> memberIdToPublicKey) {
        ExecutorService executor = Executors.newFixedThreadPool(n);
        Map<Integer, AuthenticatedPerfectLink> members = new ConcurrentHashMap<>();

        try {
            for (int i = 0; i < n; i++) {
                int index = i;
                final int port = 8080 + i;
                final String memberId = ip + ":" + port;

                executor.submit(() -> {
                    try {
                        // AuthenticatedPerfectLink member = new AuthenticatedPerfectLinkImpl(
                        //         index,
                        //         keyPairs.get(index).getPrivate(),
                        //         memberIdToPublicKey);

                        // memberes.put(index, member);

                        // member.setMessageHandler((sender, data) -> {
                        //     Logger.LOG("Member " + memberId + " received: " + new String(data));
                        // });

                        Logger.LOG("Member " + memberId + " started at port " + port);

                    } catch (Exception e) {
                        Logger.LOG("Error creating member " + memberId);
                        e.printStackTrace();
                    }
                });

                Thread.sleep(100);
            }

            Thread.sleep(1000);

            // sends messages in a ring
            for (int i = 0; i < n; i++) {
                final int senderId = i;
                final int targetId = (i + 1) % n;
                final int targetPort = 8080 + targetId;
                final String targetIdStr = ip + ":" + targetPort;

                // if (members.containsKey(senderId)) {
                //     AuthenticatedPerfectLink sender = members.get(senderId);
                //     String message = "Hello";
                //     sender.send(targetIdStr, message.getBytes());
                //     Logger.LOG("Member " + senderId + " sent message to member " + targetId);
                // }
            }

            Thread.sleep(10000);

        } catch (Exception e) {
            Logger.LOG("Error in member execution");
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
