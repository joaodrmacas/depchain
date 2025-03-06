import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

    // Generate a new AES key
    public static SecretKey generateSymmetricKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // AES-256
        return keyGen.generateKey();
    }

    // Encrypt data using AES
    public static byte[] encryptData(byte[] data, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    // Decrypt data using AES
    public static byte[] decryptData(byte[] encryptedData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encryptedData);
    }

    // Encrypt symmetric key using RSA public key
    public static byte[] encryptSymmetricKey(SecretKey secretKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(secretKey.getEncoded());
    }

    // Decrypt symmetric key using RSA private key
    public static SecretKey decryptSymmetricKey(byte[] encryptedKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(decryptedKey, "AES");
    }

    // Generate RSA Key Pair
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        return keyPairGen.generateKeyPair();
    }

    // Convert PublicKey to Base64 String
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    // Convert PrivateKey to Base64 String
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    // Convert Base64 String to PublicKey
    public static PublicKey stringToPublicKey(String key) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    // Convert Base64 String to PrivateKey
    public static PrivateKey stringToPrivateKey(String key) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
}
