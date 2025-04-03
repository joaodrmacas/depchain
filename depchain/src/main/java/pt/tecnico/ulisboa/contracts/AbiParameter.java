package pt.tecnico.ulisboa.contracts;

public class AbiParameter {
    private String name;
    private AbiType type; // Use enum for type

    public enum AbiType {
        UINT256,
        ADDRESS,
        BOOL,
        STRING,
        BYTES,
        INT256,
    }

    // Constructor
    public AbiParameter(String name, AbiType type) {
        this.name = name;
        this.type = type;
    }

    // Getters
    public String getName() {
        return name;
    }

    public AbiType getType() {
        return type;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setType(AbiType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "AbiParameter{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

}
