package pt.tecnico.ulisboa;

import pt.tecnico.ulisboa.network.AuthenticatedPerfectLinkImpl;
import pt.tecnico.ulisboa.utils.Logger;

public class Client {
    private int nodeId;
    private AuthenticatedPerfectLinkImpl authenticatedPerfectLink;
    private PrivateKey privateKey;
    private Map<Integer, PublicKey> publicKeys;

    
    public static void main(String[] args) {
    }

    public Node(int nodeId) {
        this.nodeId = nodeId;
        this.publicKeys = new HashMap<>();
    }
    
    public void setKeysDirectory(String directory) {
        this.keysDirectory = directory;
    }

    public void mainLoop() {
        while (true) {
            BFTConsensus<String> consensus = new BFTConsensus<>(
                this.authenticatedPerfectLink,
                this.nodeId
            );

            String value = consensus.start();

        }
    }

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