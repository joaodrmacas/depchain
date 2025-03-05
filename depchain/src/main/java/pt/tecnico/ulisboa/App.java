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
import pt.tecnico.ulisboa.network.layers.AuthenticatedPerfectLink;
import pt.tecnico.ulisboa.network.layers.AuthenticatedPerfectLinkImpl;
public class App {

    public static void main(String[] args) {
        final int N = 3*ConfigLoader.ALLOWED_FAILURES + 1;
        final int Q = 2*ConfigLoader.ALLOWED_FAILURES + 1;
        final String IP = "127.0.0.1";

        try {
            List<KeyPair> keyPairs = generateKeyPairs(N);
            System.out.println("Generated " + N + " key pairs successfully");

            Map<String, PublicKey> processIdToPublicKey = createProcessIdToPublicKeyMap(IP, N, keyPairs);

            startProcesses(N, IP, keyPairs, processIdToPublicKey);

        } catch (Exception e) {
            System.out.println("Error in main execution");
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

    private static Map<String, PublicKey> createProcessIdToPublicKeyMap(String ip, int n, List<KeyPair> keyPairs) {
        Map<String, PublicKey> processIdToPublicKey = new HashMap<>();

        for (int i = 0; i < n; i++) {
            int port = 8080 + i;
            String processId = ip + ":" + port;
            processIdToPublicKey.put(processId, keyPairs.get(i).getPublic());
        }

        return processIdToPublicKey;
    }

    private static void startProcesses(int n, String ip, List<KeyPair> keyPairs,
            Map<String, PublicKey> processIdToPublicKey) {
        ExecutorService executor = Executors.newFixedThreadPool(n);
        Map<Integer, AuthenticatedPerfectLink> processes = new ConcurrentHashMap<>();

        try {
            for (int i = 0; i < n; i++) {
                int index = i;
                final int port = 8080 + i;
                final String processId = ip + ":" + port;

                executor.submit(() -> {
                    try {
                        AuthenticatedPerfectLink process = new AuthenticatedPerfectLinkImpl(
                                ip,
                                port,
                                processId,
                                keyPairs.get(index).getPrivate(),
                                processIdToPublicKey);

                        processes.put(index, process);

                        process.setMessageHandler((sender, data) -> {
                            System.out.println("Process " + processId + " received: " + new String(data));
                        });

                        System.out.println("Process " + processId + " started at port " + port);

                    } catch (Exception e) {
                        System.out.println("Error creating process " + processId);
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

                if (processes.containsKey(senderId)) {
                    AuthenticatedPerfectLink sender = processes.get(senderId);
                    String message = "Hello";
                    sender.send(targetIdStr, message.getBytes());
                    System.out.println("Process " + senderId + " sent message to process " + targetId);
                }
            }

            Thread.sleep(10000);

        } catch (Exception e) {
            System.out.println("Error in process execution");
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
