package pt.tecnico.ulisboa.utils;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class CryptoUtils {

    public static SecretKey generateSymmetricKey() {
        try {
            return new SecretKeySpec(new byte[32], "HmacSHA256"); // 256-bit key for HMAC
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] generateHMAC(String data, SecretKey secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifyHMAC(String data, SecretKey secretKey, byte[] expectedHMAC) {
        try {
            byte[] calculatedHMAC = generateHMAC(data, secretKey);
            return java.util.Arrays.equals(calculatedHMAC, expectedHMAC);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Encrypt byte data with the public key
    public static byte[] encryptWithPublicKey(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data); // Encrypt the byte data
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Decrypt byte data with the private key
    public static byte[] decryptWithPrivateKey(byte[] encryptedData, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encryptedData); // Decrypt the byte data
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Encrypt a symmetric key (SecretKey) with the public key
    public static byte[] encryptSymmetricKey(SecretKey secretKey, PublicKey publicKey) {
        try {
            return encryptWithPublicKey(secretKey.getEncoded(), publicKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Decrypt the symmetric key (SecretKey) with the private key
    public static SecretKey decryptSymmetricKey(byte[] encryptedKey, PrivateKey privateKey) {
        try {
            byte[] decryptedKeyBytes = decryptWithPrivateKey(encryptedKey, privateKey);
            if (decryptedKeyBytes != null) {
                return new SecretKeySpec(decryptedKeyBytes, "HmacSHA256");
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] publicKeyToBytes(PublicKey publicKey) {
        try {
            return publicKey.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] privateKeyToBytes(PrivateKey privateKey) {
        try {
            return privateKey.getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] signData(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifySignature(String data, byte[] signatureBytes, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String bytesToBase64(byte[] bytes){
        return Base64.getEncoder().encodeToString(bytes);
    }

}
