package pt.tecnico.ulisboa.utils;

import java.io.*;

public class SerializationUtils {
    
    public static byte[] serializeObject(Serializable obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    public static <T> T deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        }
    }

    // public <T extends Serializable> void sendObject(int destId, T object) {
    //     try {
    //         byte[] serializedData = serializeObject(object);
    //         send(destId, serializedData);
    //     } catch (IOException e) {
    //         System.err.println("Failed to serialize object: " + e.getMessage());
    //         e.printStackTrace();
    //     }
    // }
    
}