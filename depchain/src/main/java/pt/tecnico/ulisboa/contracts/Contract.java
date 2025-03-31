package pt.tecnico.ulisboa.contracts;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.EvmSpecVersion;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

public class Contract {

    private String RUNTIME_CODE;
    private String DEPLOY_CODE;

    protected static Map<String, String> METHOD_SIGNATURES;

    protected final Address address;
    protected final EVMExecutor executor;
    protected final ByteArrayOutputStream output;
    protected final StandardJsonTracer tracer;

    public Contract(Address address, WorldUpdater world) {
        // Generate contract address
        this.address = address;

        // Create contract account
        world.createAccount(address, 0, Wei.fromEth(0));

        // Create objects to catch output
        // TODO: o tracer é para dar memory state, stack e de gas, pode ser util mas a
        // partida nao é preciso
        this.executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
        this.output = new ByteArrayOutputStream();
        this.tracer = new StandardJsonTracer(new PrintStream(output), true, true, true, true);

        executor.tracer(tracer);
        executor.receiver(address);
        executor.worldUpdater(world.updater());
        executor.commitWorldState();
    }

    public void setByteCode(String byteCode) {
        Bytes b = Bytes.fromHexString(byteCode);
        executor.code(b);
    }

    public Address getAddress() {
        return address;
    }

    public String getRuntimeCode() {
        return RUNTIME_CODE;
    }

    public String getDeployCode() {
        return DEPLOY_CODE;
    }

}
