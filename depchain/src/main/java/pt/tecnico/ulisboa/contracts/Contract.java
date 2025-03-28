package pt.tecnico.ulisboa.contracts;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import pt.tecnico.ulisboa.Config;

public abstract class Contract {

    protected static Map<String, String> METHOD_SIGNATURES;

    protected final Address contractAddress;
    protected final EVMExecutor executor;
    protected final ByteArrayOutputStream output;
    protected final StandardJsonTracer tracer;
    protected final SimpleWorld world;

    public Contract(SimpleWorld world) {
        // Generate contract address
        this.contractAddress = Address.fromHexString(Config.MERGED_CONTRACT_ADDRESS);
        this.world = world;

        // Create contract account
        world.createAccount(contractAddress, 0, Wei.fromEth(0));

        // Create objects to catch output
        // TODO: o tracer é para dar memory state,stack e de gas, pode ser util mas a
        // partida nao é preciso
        this.executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        this.output = new ByteArrayOutputStream();
        this.tracer = new StandardJsonTracer(new PrintStream(output), true, true, true, true);

        executor.tracer(tracer);
        executor.sender(Config.CLIENT_ID_2_ADDR.get(Config.ADMIN_ID));
        executor.receiver(contractAddress);
    }

    public void setByteCode(String byteCode) {
        Bytes b = Bytes.fromHexString(byteCode);
        executor.code(b);
    }

    public Address getAddress() {
        return contractAddress;
    }

    public abstract void deploy(Object... args);

}
