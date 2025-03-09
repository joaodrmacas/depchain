package pt.tecnico.ulisboa;

import pt.tecnico.ulisboa.consensus.BFTConsensus;
import pt.tecnico.ulisboa.network.AuthenticatedPerfectLinkImpl;
import pt.tecnico.ulisboa.utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Queue;

public class Node {
    private int nodeId;
    private PrivateKey privateKey;
    private HashMap<Integer, PublicKey> publicKeys;
    private AuthenticatedPerfectLinkImpl authenticatedPerfectLink;
    private String keysDirectory = "keys"; // Default directory TODO: should be in config file 
    private Queue<Integer> txQueue;
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Node <node-id> [keys-directory]");
            System.exit(1);
        }

        try {
            int nodeId = Integer.parseInt(args[0]);
            
            Node node = new Node(nodeId);
            
            if (args.length >= 2) {
                node.setKeysDirectory(args[1]);
            }
            
            node.setup();
            
            // node.mainLoop();

        } catch (NumberFormatException e) {
            System.err.println("Error: Node ID must be an integer");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error during node setup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Node(int nodeId) {
        this.nodeId = nodeId;
        this.publicKeys = new HashMap<>();
    }
    
    public void setKeysDirectory(String directory) {
        this.keysDirectory = directory;
    }

    // public void mainLoop() {
    //     while (true) {
    //         BFTConsensus<Integer> consensus = new BFTConsensus<>(
    //             this.authenticatedPerfectLink,
    //             this.nodeId
    //         );

    //         String value = consensus.start(this.txQueue.peek());

    //     }
    // }

    public void setup() {
        try {
            // Read private key for this node
            readPrivateKey();
            
            // Read all public keys
            readAllPublicKeys();
            
            // Initialize network and consensus components
            authenticatedPerfectLink = new AuthenticatedPerfectLinkImpl(nodeId, privateKey, publicKeys);
            if (nodeId == 0){
                authenticatedPerfectLink.send(1, "Hello".getBytes());
            }
            
            Logger.LOG("Node " + nodeId + " successfully initialized with " + 
                              publicKeys.size() + " public keys");
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


        for (int i=0; i<keyFiles.length; i++) {
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
}