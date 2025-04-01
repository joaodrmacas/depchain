// TODO: is sender the allower or allowee

package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;

import pt.tecnico.ulisboa.utils.ContractUtils;

// Get spending allowance between two accounts
public class ContractCallReq extends ClientReq {
    private static final long serialVersionUID = 1L;
    private final BigInteger value;
    private final Address contractAddr;
    private final String methodSelector;
    private final String[] args;

    public ContractCallReq(int senderId, Long count, Address contractAddr, String methodSelector, BigInteger value,
            Object... args) {
        super(senderId, count);

        this.contractAddr = contractAddr;
        this.value = value;
        this.methodSelector = methodSelector;
        this.args = parseArgs(args);

    }

    public ContractCallReq(int senderId, Long count, Address contractAddr, String methodSelector) {
        this(senderId, count, contractAddr, methodSelector, BigInteger.ZERO, new Object[] {});
    }

    public ContractCallReq(int senderId, Long count, Address contractAddr, String methodSelector, Object... args) {
        this(senderId, count, contractAddr, methodSelector, BigInteger.ZERO, args);
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
        // TODO: check if this is correct
        String[] hexArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof BigInteger) {
                hexArgs[i] = ((BigInteger) args[i]).toString(16);
            } else if (args[i] instanceof Address) {
                hexArgs[i] = ((Address) args[i]).toHexString();
            } else {
                throw new IllegalArgumentException("Unsupported argument type: " + args[i].getClass().getName());
            }
        }
        return hexArgs;
    }

    public Address getContractAddr() {
        return contractAddr;
    }

    public BigInteger getValue() {
        return value;
    }

    public Bytes getCallData() {
        // TODO: check if the method selector needs to be padded. (It prob does)
        StringBuilder callData = new StringBuilder(ContractUtils.padHexStringTo256Bit(methodSelector));
        for (String arg : args) {
            // Convert the argument to a 256-bit hex string and append it to the callData
            String paddedArg = ContractUtils.padHexStringTo256Bit(arg);
            callData.append(paddedArg);
        }
        return Bytes.fromHexString(callData.toString());
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
                ", contractAddr='" + contractAddr + '\'' +
                ", methodSelector='" + methodSelector + '\'' +
                ", value=" + value +
                ", args=" + String.join(", ", args) +
                '}';
    }
}