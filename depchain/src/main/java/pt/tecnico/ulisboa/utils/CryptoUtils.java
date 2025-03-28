package pt.tecnico.ulisboa.utils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static KeyPair generateKeyPair(int size) {
        try {
            java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(size);
            return keyGen.generateKeyPair();
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

    public static String signData(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifySignature(String data, String base64Signature, PublicKey publicKey) {

        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(base64Signature));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String hashSHA256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input); // Directly pass the byte array
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static PublicKey bytesToPublicKey(byte[] keyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
