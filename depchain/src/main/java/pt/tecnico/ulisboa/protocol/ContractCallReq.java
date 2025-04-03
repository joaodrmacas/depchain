package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;

import pt.tecnico.ulisboa.utils.ContractUtils;
import pt.tecnico.ulisboa.utils.types.Logger;

// Get spending allowance between two accounts
public class ContractCallReq extends ClientReq {
    private static final long serialVersionUID = 1L;
    private final BigInteger value;
    private final String contractName; // Changed from contractAddr
    private final String methodName; // Changed from methodSelector
    private final String[] args;

    public ContractCallReq(int senderId, Long count, String contractName, String methodName, BigInteger value,
            Object... args) {
        super(senderId, count);

        this.contractName = contractName;
        this.value = value;
        this.methodName = methodName;
        this.args = parseArgs(args);
    }

    public ContractCallReq(int senderId, Long count, String contractName, String methodName) {
        this(senderId, count, contractName, methodName, BigInteger.ZERO, new Object[] {});
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

    public BigInteger getValue() {
        return value;
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
    public boolean needsConsensus() {
        return true;
    }
}
