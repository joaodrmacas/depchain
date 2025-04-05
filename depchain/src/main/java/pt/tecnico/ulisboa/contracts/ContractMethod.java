package pt.tecnico.ulisboa.contracts;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;

public class ContractMethod {
    private String signature; // hexadecimal signature
    private List<AbiParameter> outputs; // Sao varios pq as solidity functions podem retornar varios valores
    private boolean changesState; // se a funcao altera o estado da blockchain ou nao

    // Constructor
    public ContractMethod(String signature, List<AbiParameter> outputs, boolean changesState) {
        this.signature = signature;
        this.outputs = outputs;
        this.changesState = changesState;
    }

    // Getters
    public Bytes getSignature() {
        return Bytes.fromHexString(signature);
    }

    public List<AbiParameter> getOutputs() {
        return outputs;
    }

    public boolean changesState() {
        return changesState;
    }

    @Override
    public String toString() {
        return "ContractMethod{" +
                "signature='" + signature + '\'' +
                ", outputs=" + outputs +
                ", changesState=" + changesState +
                '}';
    }
}
