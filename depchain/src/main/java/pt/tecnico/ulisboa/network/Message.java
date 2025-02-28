package pt.tecnico.ulisboa.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class Message {
    private String key;
    final String destination;
    final int port;
    final byte[] content;
    private long cooldown = 1;
    private long counter = 1;

    Message(String destination, int port, byte[] content) {
        this.destination = destination;
        this.port = port;
        this.content = content;
    }

    String getKey() {
        return key;
    }

    long getCounter() {
        return counter;
    }

    long getCooldown() {
        return cooldown;
    }

    void setKey(String key) {
        this.key = key;
    }

    void setCounter(long counter) {
        this.counter = counter;
    }

    void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    void incrementCounter() {
        counter++;
    }

    void doubleCooldown() {
        // TODO: manter um CAP?
        cooldown = Math.min(cooldown * 2, 10000000);
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                DataOutputStream dataStream = new DataOutputStream(byteStream)) {

            dataStream.writeUTF(destination);

            dataStream.writeInt(port);

            dataStream.writeInt(content.length);
            dataStream.write(content);

            return byteStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Message deserialize(byte[] data) {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
                DataInputStream dataStream = new DataInputStream(byteStream)) {

            String destination = dataStream.readUTF();

            int port = dataStream.readInt();

            int length = dataStream.readInt();
            byte[] content = new byte[length];
            dataStream.readFully(content);

            return new Message(destination, port, content);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}