package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class AuthenticatedMessage extends Message {
    private final byte[] hmac;

    public AuthenticatedMessage(byte[] content, long seqNum, byte[] hmac) {
        super(content, seqNum);
        this.hmac = hmac;
    }

    public byte[] getMac() {
        return hmac;
    }

    public abstract byte getType();

    // TODO: This is shit cause it forces ack messages to have content, maybe move
    // to the children
    // por mim é bue ok ter content vazio - Massas
    @Override
    public byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write message type
            dos.writeByte(getType());

            // Write message fields
            dos.writeLong(getSeqNum());

            // Write content
            dos.writeInt(getContent().length);
            dos.write(getContent());

            // Write MAC
            dos.writeInt(hmac.length);
            dos.write(hmac);

            dos.flush();

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    // protected byte[] generateMac(Key secretKey) throws NoSuchAlgorithmException,
    // InvalidKeyException {
    // Mac mac = Mac.getInstance("HmacSHA256");
    // mac.init(new SecretKeySpec(secretKey.getEncoded(), "HmacSHA256"));

    // ByteArrayOutputStream dataToAuthenticate = new ByteArrayOutputStream();
    // try (DataOutputStream dos = new DataOutputStream(dataToAuthenticate)) {
    // dos.writeUTF(getSenderName());
    // dos.writeInt(getSenderPort());
    // dos.writeLong(getSeqNum());
    // dos.writeInt(getContent().length);

    // dos.write(getContent());
    // } catch (Exception e) {
    // throw new RuntimeException("Failed to generate data for MAC", e);
    // }

    // return mac.doFinal(dataToAuthenticate.toByteArray());
    // }

    // public boolean verifyMac(Key secretKey) {
    // try {
    // byte[] expectedMac = generateMac(secretKey);

    // // Compare MACs for equality
    // if (hmac.length != expectedMac.length) {
    // return false;
    // }

    // // Constant-time comparison to prevent timing attacks
    // int result = 0;
    // for (int i = 0; i < hmac.length; i++) {
    // result |= hmac[i] ^ expectedMac[i];
    // }

    // return result == 0;
    // } catch (Exception e) {
    // System.err.println("MAC verification failed: " + e.getMessage());
    // return false;
    // }
    // }

}