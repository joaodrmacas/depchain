package pt.tecnico.ulisboa;

import java.io.*;
import org.hyperledger.besu.evm.fluent.SimpleWorld;

//TODO: this needs to be changed
public class State implements Serializable {
    private static final long serialVersionUID = 1L;

    private final SimpleWorld simpleWorld;

    public State(SimpleWorld simpleWorld) {
        this.simpleWorld = simpleWorld;
    }

    public SimpleWorld getSimpleWorld() {
        return simpleWorld;
    }

    public State deepCopy() throws IOException, ClassNotFoundException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            
            oos.writeObject(this);
            
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                 ObjectInputStream ois = new ObjectInputStream(bais)) {
                
                return (State) ois.readObject();
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}