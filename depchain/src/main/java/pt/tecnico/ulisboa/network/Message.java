package pt.tecnico.ulisboa.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final byte[] EOF_INDICATOR = "EOF".getBytes();
    private String id;
    final String content;
    private String destination;
    final int port;
    private long seq_num;

    private long send_cooldown = 1;
    private long send_counter = 1;

    Message(String destination, int port, String content, long seq_num) {
        this.destination = destination;
        this.port = port;
        this.content = content;
        this.seq_num = seq_num;
        this.id = destination + ":" + port + ":" + seq_num;
    }

    int getPort() {
        return port;
    }

    String getContent() {
        return content;
    }

    String getDestination() {
        return destination;
    }

    long getSeqNum() {
        return seq_num;
    }

    String getId() {
        return id;
    }

    long getCounter() {
        return send_counter;
    }

    long getCooldown() {
        return send_cooldown;
    }

    void setKey(String key) {
        this.id = key;
    }

    void setCounter(long send_counter) {
        this.send_counter = send_counter;
    }

    void setCooldown(long send_cooldown) {
        this.send_cooldown = send_cooldown;
    }

    void incrementCounter() {
        send_counter++;
    }

    void doubleCooldown() {
        send_cooldown = Math.min(send_cooldown * 2, 10000000);
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream dataStream = new DataOutputStream(byteStream)) {

            dataStream.writeUTF(destination);
            dataStream.writeInt(port);
            dataStream.writeLong(seq_num);
            dataStream.writeInt(content.length);

            // Write content
            Byte[] content_in_bytes = new Byte[this.content.length()];
            dataStream.write(content_in_bytes);
            
            // Write EOF indicator
            dataStream.write(EOF_INDICATOR);

            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Message deserialize(byte[] data) {
        if (data == null || data.length == 0) {
            System.err.println("Error: Attempted to deserialize null or empty data.");
            return null;
        }

        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             DataInputStream dataStream = new DataInputStream(byteStream)) {

            if (dataStream.available() < 16 + EOF_INDICATOR.length) {
                System.err.println("Error: Insufficient data for deserialization.");
                return null;
            }

            String destination = dataStream.readUTF();
            int port = dataStream.readInt();
            long seq_num = dataStream.readLong();
            int length = dataStream.readInt();

            if (length < 0 || length > dataStream.available() - EOF_INDICATOR.length) {
                System.err.println("Error: Invalid content length.");
                return null;
            }

            byte[] content = new byte[length];
            dataStream.readFully(content);

            // Check for EOF indicator
            byte[] eofCheck = new byte[EOF_INDICATOR.length];
            dataStream.readFully(eofCheck);
            if (!java.util.Arrays.equals(eofCheck, EOF_INDICATOR)) {
                System.err.println("Error: EOF indicator not found.");
                return null;
            }

            return new Message(destination, port, content, seq_num);

        } catch (EOFException e) {
            System.err.println("Error: Unexpected end of file during deserialization.");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error during deserialization: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
