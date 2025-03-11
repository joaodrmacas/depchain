package pt.tecnico.ulisboa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.ulisboa.consensus.BFTConsensus;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessage;
import pt.tecnico.ulisboa.consensus.message.ConsensusMessageHandler;
import pt.tecnico.ulisboa.network.APLImpl;
import pt.tecnico.ulisboa.protocol.AppendResp;
import pt.tecnico.ulisboa.protocol.BlockchainMessage;
import pt.tecnico.ulisboa.utils.Logger;
import pt.tecnico.ulisboa.utils.ObservedResource;
import pt.tecnico.ulisboa.utils.RequiresEquals;


public class Node<T extends RequiresEquals> {
    private int nodeId;
    private PrivateKey privateKey;
    private HashMap<Integer, PublicKey> publicKeys;
    private HashMap<Integer, APLImpl> apls;
    private HashMap<Integer, APLImpl> clientApls;
    private String keysDirectory = Config.DEFAULT_KEYS_DIR;
    private ConcurrentHashMap<Integer, PublicKey> clientPublicKeys;
    private ConcurrentLinkedQueue<T> transactions;
    private ObservedResource<Queue<T>> decidedValues;
    private Map<Integer, ObservedResource<Queue<ConsensusMessage<T>>>> consensusMessages = new HashMap<>();
    private ArrayList<T> blockchain;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Node <node-id> [keys-directory]");
            System.exit(1);
        }

        try {
            int nodeId = Integer.parseInt(args[0]);
            Node<BlockchainMessage> node = new Node<BlockchainMessage>(nodeId);

            if (args.length >= 2) {
                node.setKeysDirectory(args[1]);
            }

            node.setup();
            node.mainLoop();

        } catch (Exception e) {
            Logger.ERROR("Node setup failed", e);
            System.exit(1);
        }
    }

    public Node(int nodeId) {
        this.nodeId = nodeId;
        this.publicKeys = new HashMap<>();

    }

    public int getId() {
        return nodeId;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public HashMap<Integer, APLImpl> getLinks() {
        return apls;
    }

    public HashMap<Integer, APLImpl> getClientLinks() {
        return clientApls;
    }

    public APLImpl getLink(int destId) {
        if (destId == nodeId) {
            Logger.ERROR("Cannot create a link to self");
        }

        return apls.get(destId);
    }

    public APLImpl getClientLink(int destId) {
        if (destId == nodeId) {
            Logger.ERROR("Cannot create a link to self");
        }

        return clientApls.get(destId);
    }

    public Map<Integer, PublicKey> getPublicKeys() {
        return publicKeys;
    }

    public void setKeysDirectory(String directory) {
        this.keysDirectory = directory;
    }

    public void mainLoop() {

        Thread valueHandlerThread = new Thread(() -> {
            try {
                while (true) {
                    // consensus thread already changes the decided queue
                    decidedValues.waitForChange(Integer.MAX_VALUE);
                    T value = decidedValues.getResource().poll();
                    handleDecidedValue(value);
                }
            } catch (Exception e) {
                Logger.ERROR("Value handler thread failed with exception", e);
                // TODO: Handle recovery or shutdown as appropriate (this should not happen tho)
            }
        });

        BFTConsensus<T> consensusLoop = new BFTConsensus<>(this);
        Thread consensusThread = new Thread(() -> {
            try {
                consensusLoop.start();
            } catch (Exception e) {
                Logger.ERROR("Consensus thread failed with exception", e);
                // TODO: Handle recovery or shutdown as appropriate (this should not happen tho)
            }
        });



        consensusThread.setName("BFT-Consensus-Thread");
        valueHandlerThread.setName("Value-Handler-Thread");

        consensusThread.start();
        valueHandlerThread.start();

    }

    private void handleDecidedValue(T value) {
        boolean success = false;
        //TODO: verificar se foi um abort. Se sim, temos de mandar false no success ao cliente.
        if (value != null) {
            success = true;
            //Add to blockchain
            blockchain.add(value);
            Logger.LOG("Decided value: " + value);
        }

        LocalDateTime timestamp = LocalDateTime.now();

        //Send answer to clientstrue
        clientApls.get(value.getSenderId()).send(new AppendResp(success,timestamp));
    }

    public void setup() {
        Logger.LOG("Setting up node " + nodeId);
        try {
            // Read private key for this node
            readPrivateKey();

            // Read all public keys
            readAllPublicKeys();

            // Initialize the consensus messages queues
            for (int destId : publicKeys.keySet()) {
                if (destId == nodeId) {
                    continue;
                }
                this.consensusMessages.put(destId, new ObservedResource<>(new LinkedList<>()));
            }

            // Initialize APLs, one for each destination node
            for (int destId : publicKeys.keySet()) {
                if (destId == nodeId) {
                    continue;
                }

                APLImpl apl = new APLImpl(nodeId, destId, privateKey, publicKeys.get(destId));
                apl.setMessageHandler(new ConsensusMessageHandler<T>(consensusMessages));
                apls.put(destId, apl);
                Logger.LOG("APL created for destination node " + destId);
            }

            //TODO: assuming Config.NUM_CLIENTS will exist. Change later for a register link?
            //TODO: precisamos de conseguir criar links para clientes - massas
            // Need a clientid to port translation - Massas
            for (int i=0; i<Config.NUM_CLIENTS; i++) {
                APLImpl apl = new APLImpl(nodeId, i, privateKey, publicKeys.get(i));
                apl.setMessageHandler(new NodeMessageHandler<T>(transactions, clientPublicKeys));
                clientApls.put(i, apl);
                Logger.LOG("APL created for client node " + i);
            }

            Logger.LOG("Node setup complete");

            // if (nodeId == 0) {
            // WriteTuple<String> writeTuple = new WriteTuple<>("Hello", 0);
            // WriteMessage<String> writeMessage = new WriteMessage<>(writeTuple);
            // apls.get(1).send(1, writeMessage);
            // }

        } catch (Exception e) {
            System.err.println("Setup failed: " + e.getMessage());
            throw new RuntimeException("Node setup failed", e);
        }
    }

    private void readPrivateKey() throws Exception {
        String privateKeyPath = String.format("%s/priv%02d.key", keysDirectory, nodeId);
        Logger.LOG("Reading private key from: " + privateKeyPath);

        File privateKeyFile = new File(privateKeyPath);
        if (!privateKeyFile.exists()) {
            throw new RuntimeException("Private key file not found: " + privateKeyFile.getAbsolutePath());
        }

        // Read the PEM format key
        String pemKey = readPemFile(privateKeyFile);

        // Extract the base64 encoded key data (remove PEM headers and newlines)
        String base64Key = pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        // Decode the base64 key data
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);

        // Create the private key
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKey = keyFactory.generatePrivate(spec);
    }

    private void readAllPublicKeys() throws Exception {
        File keysDir = new File(keysDirectory);
        if (!keysDir.exists() || !keysDir.isDirectory()) {
            throw new RuntimeException("Keys directory not found: " + keysDir.getAbsolutePath());
        }

        Logger.LOG("Reading public keys from: " + keysDir.getAbsolutePath());

        File[] keyFiles = keysDir.listFiles((dir, name) -> name.startsWith("pub") && name.endsWith(".key"));
        if (keyFiles == null || keyFiles.length == 0) {
            throw new RuntimeException("No public keys found in " + keysDir.getAbsolutePath());
        }

        for (int i = 0; i < keyFiles.length; i++) {
            String keyPath = keyFiles[i].getPath();
            String keyName = keyFiles[i].getName();
            int keyId = Integer.parseInt(keyName.substring(3, 5));
            Logger.LOG("Reading public key from: " + keyPath);

            // Read the PEM format key
            String pemKey = readPemFile(keyFiles[i]);

            // Extract the base64 encoded key data (remove PEM headers and newlines)
            String base64Key = pemKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode the base64 key data
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);

            // Create the public key
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);
            publicKeys.put(keyId, publicKey);
        }
    }

    private String readPemFile(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public T fetchReceivedTx() {
        return transactions.poll();
    }

    public void pushDecidedTx(T value) {
        decidedValues.getResource().add(value);
        decidedValues.notify();
    }

    public ConsensusMessage<T> fetchConsensusMessageOrWait(int senderId) {
        while (true) {
            // gotta decide if its peek() or poll() (keep or remove)
            ConsensusMessage<T> msg = consensusMessages.get(senderId).getResource().poll();
            if (msg != null) {
                return msg;
            }

            try {
                boolean hasTimeouted = !consensusMessages.get(senderId).waitForChange(Config.LINK_TIMEOUT);

                if (hasTimeouted)
                    return null;
            } catch (Exception e) {
                Logger.ERROR("Exception: ", e);
            }
        }
    }

    public void removeFirstConsensusMessage(int senderId) {
        consensusMessages.get(senderId).getResource().poll();
    }
}