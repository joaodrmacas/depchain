package pt.tecnico.ulisboa.utils;

import java.io.*;

public class SerializationUtils {
    
    // Convert a Serializable object to a byte array
    public static byte[] toByteArray(Serializable object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        
        try {
            oos.writeObject(object);
            return baos.toByteArray();
        } finally {
            oos.close();
            baos.close();
        }
    }
    
    // Convert a byte array back to an object
    public static <T> T fromByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        
        try {
            @SuppressWarnings("unchecked")
            T object = (T) ois.readObject();
            return object;
        } finally {
            ois.close();
            bais.close();
        }
    }
}