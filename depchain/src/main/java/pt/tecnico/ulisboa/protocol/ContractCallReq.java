package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;

import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.types.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

// Get spending allowance between two accounts
public class ContractCallReq extends ClientReq {
    private static final long serialVersionUID = 1L;
    private BigInteger value;
    private String contractName; // Changed from contractAddr
    private String methodName; // Changed from methodSelector
    private String[] args;

    public ContractCallReq() {
        // For json
        super();
        this.args = new String[0];
        this.value = BigInteger.ZERO;
    }

    public ContractCallReq(int senderId, Long count, String contractName, String methodName, BigInteger value,
            Object... args) {
        super(senderId, count, ClientReqType.CONTRACT_CALL);

        this.contractName = contractName;
        this.value = value;
        this.methodName = methodName;
        this.args = parseArgs(args);
    }

    public ContractCallReq(int senderId, Long count, String contractName, String methodName, Object... args) {
        this(senderId, count, contractName, methodName, BigInteger.ZERO, args);
    }

    /**
     * Converts the arguments to a hex string representation.
     * 
     * @param args The arguments to be converted.
     * @return An array of hex strings representing the arguments.
     * @throws IllegalArgumentException if an unsupported argument type is
     *                                  encountered.
     */
    private String[] parseArgs(Object... args) {
        String[] hexArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof BigInteger) {
                hexArgs[i] = ((BigInteger) args[i]).toString(16);
            } else if (args[i] instanceof Address) {
                hexArgs[i] = ((Address) args[i]).toHexString();
            } else if (args[i] instanceof String) { // already in hex format
                hexArgs[i] = (String) args[i];
            } else {
                throw new IllegalArgumentException("Unsupported argument type: " + args[i].getClass().getName());
            }
        }
        return hexArgs;
    }

    public String getContractName() {
        return contractName;
    }

    public String getMethodName() {
        return methodName;
    }

    public Wei getValue() {
        // Convert the value to Wei (1 Dep = 10^18 Wei)
        return Wei.of(value.multiply(BigInteger.TEN.pow(18)));
    }

    public Bytes getArgs() {
        StringBuilder argsString = new StringBuilder();
        for (String arg : args) {
            // Convert the argument to a 256-bit hex string and append it to the argsString
            String paddedArg = ContractUtils.padHexStringTo256Bit(arg);
            argsString.append(paddedArg);
        }
        return Bytes.fromHexString(argsString.toString());
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.CONTRACT_CALL;
    }

    @Override
    public String toString() {
        return "ContractCallReq{" +
                "senderId=" + senderId +
                ", count=" + count +
                ", contractName='" + contractName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", value=" + value +
                ", args=" + String.join(", ", args) +
                '}';
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("contractName", contractName);
        json.addProperty("methodName", methodName);
        json.addProperty("value", value.toString());

        // Convert args array to a JSON array instead of a comma-separated string
        if (args != null && args.length > 0) {
            JsonArray argsArray = new JsonArray();
            for (String arg : args) {
                argsArray.add(arg);
            }
            json.add("argsArray", argsArray);
        } else {
            json.add("argsArray", new JsonArray());
            json.addProperty("args", "");
        }

        return json;
    }

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
        this.contractName = json.get("contractName").getAsString();
        this.methodName = json.get("methodName").getAsString();
        this.value = new BigInteger(json.get("value").getAsString());

        if (json.has("argsArray") && json.get("argsArray").isJsonArray()) {
            JsonArray argsArray = json.getAsJsonArray("argsArray");
            this.args = new String[argsArray.size()];
            for (int i = 0; i < argsArray.size(); i++) {
                this.args[i] = argsArray.get(i).getAsString();
            }
        } else if (json.has("args") && !json.get("args").getAsString().isEmpty()) {
            this.args = json.get("args").getAsString().split(", ");
        } else {
            this.args = new String[0];
        }
    }
}
