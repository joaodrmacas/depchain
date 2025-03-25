package pt.tecnico.ulisboa.contracts;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;
import org.hyperledger.besu.datatypes.Wei;
import org.apache.tuweni.bytes.Bytes;

import pt.tecnico.ulisboa.utils.ContractUtils;

public abstract class Contract {
    protected String BYTECODE;
    
    protected static Map<String, String> METHOD_SIGNATURES;

    protected final Address contractAddress;
    protected final EVMExecutor executor;
    protected final ByteArrayOutputStream output;
    protected final StandardJsonTracer tracer;
    protected final SimpleWorld world;

    public Contract(SimpleWorld world, Address owner) {        
        // Generate contract address
        this.contractAddress = ContractUtils.generateContractAddress(owner);
        this.world = world;
        world.createAccount(contractAddress, 0, Wei.fromEth(0));
        
        // Create contract account
        world.createAccount(contractAddress, 0, Wei.fromEth(0));

        //Create objects to catch output
        //TODO: o tracer é para dar memory state,stack e de gas, pode ser util mas a partida nao é preciso
        this.output = new ByteArrayOutputStream();
        this.tracer = new StandardJsonTracer(new PrintStream(output), true, true, true, true);


        this.executor = EVMExecutor.evm();
        executor.tracer(tracer);
        executor.code(Bytes.fromHexString(BYTECODE));
        executor.sender(owner); //TODO: assuming there's only one admin account
        executor.receiver(contractAddress);

    }

}
