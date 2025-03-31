// TODO: is sender the allower or allowee

package pt.tecnico.ulisboa.protocol;

import java.math.BigInteger;

import org.hyperledger.besu.datatypes.Address;

import pt.tecnico.ulisboa.utils.ContractUtils;

// Get spending allowance between two accounts
public class ContractCallReq extends ClientReq {
    private static final long serialVersionUID = 1L;
    private final BigInteger value;
    private final Address contractAddr;
    private final String methodSelector; // hex string of the method selector
    private String[] args; // arguments to the method hex string encoded

    public ContractCallReq(int senderId, Long count, BigInteger value, Address contractAddr, String methodSelector,
            String... args) {
        super(senderId, count);

        this.value = value;
        this.contractAddr = contractAddr;
        this.methodSelector = methodSelector;
        this.args = args;
    }

    public Address getContractAddr() {
        return contractAddr;
    }

    public String getMethodSelector() {
        return methodSelector;
    }

    public String getArgs() {
        // return the hextring of all arguments comvined into one and padded to 256 bits each
        StringBuilder argsString = new StringBuilder();
        for (String arg : args) {
            argsString.append(ContractUtils.padHexStringTo256Bit(arg));
        }
        return argsString.toString();
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public ClientReqType getReqType() {
        return ClientReqType.CONTRACT_CALL;
    }

    @Override
    public String toString() {
        StringBuilder argsString = new StringBuilder();
        for (Object arg : args) {
            if (arg instanceof byte[]) {
                argsString.append("0x").append(new BigInteger(1, (byte[]) arg).toString(16)).append(", ");
            } else if (arg instanceof String) {
                argsString.append(arg).append(", ");
            } else {
                argsString.append(arg.toString()).append(", ");
            }
        }
        return "ContractReq{" +
                "senderId=" + getSenderId() +
                ", count=" + getCount() +
                ", value=" + value +
                ", contractAddr=" + contractAddr +
                ", methodSelector=" + new String(methodSelector) +
                ", args=[" + argsString.toString() + "]" +
                '}';
    }
}