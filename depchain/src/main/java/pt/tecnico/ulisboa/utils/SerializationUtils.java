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

    @SuppressWarnings("unchecked")
    public static <T> T deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object obj = ois.readObject(); // Read the object
            return (T) obj; // Return the object
        }
    }

}