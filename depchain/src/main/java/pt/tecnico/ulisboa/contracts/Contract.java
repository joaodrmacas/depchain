package pt.tecnico.ulisboa.contracts;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import org.apache.tuweni.bytes.Bytes;
import org.checkerframework.checker.units.qual.A;
import org.hyperledger.besu.datatypes.Address;

public class Contract {
    protected final String address; // hex string of the address
    private static HashMap<String, ContractMethod> functions;

    public Contract(String address, HashMap<String, ContractMethod> functions) {
        this.address = address;
        this.functions = functions;
    }

    public Address getAddress() {
        return Address.fromHexString(address);
    }

    public HashMap<String, ContractMethod> getMethods() {
        return functions;
    }

    public ContractMethod getMethod(String functionName) {
        return functions.get(functionName);
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        ps.println("Contract{" +
                "address='" + address + '\'' +
                ", functions=" + functions +
                '}');
        return baos.toString();
    }
}
