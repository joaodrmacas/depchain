package pt.tecnico.ulisboa.network.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class AuthenticatedMessage extends Message {
    private byte[] hmac;

    public AuthenticatedMessage(byte[] content, long seqNum, byte[] hmac) {
        super(content, seqNum);
        this.hmac = hmac;
    }

    public byte[] getMac() {
        return hmac;
    }

    public void setHmac(byte[] hmac) {
        this.hmac = hmac;
    }

    public abstract byte getType();

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

    @Override
    public String toString() {
        return "AuthenticatedMessage{" + "type=" + getType() + ", seqNum=" + getSeqNum() + ", content=" + new String(getContent()) + ", hmac=" + hmac + '}';
    }

}