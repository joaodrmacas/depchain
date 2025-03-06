package pt.tecnico.ulisboa.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.nio.charset.StandardCharsets;

public class CryptoUtils {

    public static SecretKey generateSymmetricKey() throws Exception {
        return new SecretKeySpec(new byte[32], "HmacSHA256"); // 256-bit key for HMAC
    }

    public static byte[] generateHMAC(String data, SecretKey secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);

        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] encryptWithPublicKey(SecretKey secretKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(secretKey.getEncoded());
    }

    public static byte[] decryptWithPrivateKey(byte[] encryptedKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(encryptedKey);
    }

    public static SecretKey decryptSecretKey(byte[] encryptedKey, PrivateKey privateKey) throws Exception {
        byte[] decryptedKeyBytes = decryptWithPrivateKey(encryptedKey, privateKey);
        return new SecretKeySpec(decryptedKeyBytes, "HmacSHA256");
    }

    public static byte[] encryptSecretKey(SecretKey secretKey, PublicKey publicKey) throws Exception {
        return encryptWithPublicKey(secretKey, publicKey);
    }

    public static byte[] publicKeyToBytes(PublicKey publicKey) {
        return publicKey.getEncoded();
    }

    public static byte[] privateKeyToBytes(PrivateKey privateKey) {
        return privateKey.getEncoded();
    }

     public static byte[] signData(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return signature.sign();
    }

    public static boolean verifySignature(String data, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return signature.verify(signatureBytes);
    }

}
