package pt.tecnico.ulisboa.contracts;

import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.apache.tuweni.bytes.Bytes;
import java.math.BigInteger;

import pt.tecnico.ulisboa.utils.ContractUtils;


public class DepCoinContract extends Contract {

    public DepCoinContract(SimpleWorld world, Address owner) {
        super(world,owner);
        //TODO: fill these
        METHOD_SIGNATURES = Map.of(
            "transfer", "AAAAA",
            "transferFrom", "AAAAA",
            "approve", "AAAAA",
            "balanceOf", "AAAAA"
        );
        //TODO: fill these
        super.BYTECODE = "AAAAA";
    }

    //TODO: testar
    public void transfer(Address recipient, BigInteger amount) {
        String encodedRecipient = ContractUtils.padAddressTo256Bit(recipient.toHexString());
        String encodedAmount = ContractUtils.padBigIntegerTo256Bit(amount);
        
        executor.worldUpdater(world.updater()); 
        executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("transfer") + encodedRecipient + encodedAmount));
        executor.execute();
        executor.commitWorldState(); 
    }
    
    //TODO: testar
    public void transferFrom(Address from, Address to, BigInteger amount) {
        String encodedFrom = ContractUtils.padAddressTo256Bit(from.toHexString());
        String encodedTo = ContractUtils.padAddressTo256Bit(to.toHexString());
        String encodedAmount = ContractUtils.padBigIntegerTo256Bit(amount);
        
        executor.worldUpdater(world.updater());
        executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("transferFrom") + encodedFrom + encodedTo + encodedAmount));
        executor.execute();
        executor.commitWorldState();
    }
    
    //TODO: testar
    public void approve(Address spender, BigInteger amount) {
        String encodedSpender = ContractUtils.padAddressTo256Bit(spender.toHexString());
        String encodedAmount = ContractUtils.padBigIntegerTo256Bit(amount);
        
        executor.worldUpdater(world.updater());
        executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("approve") + encodedSpender + encodedAmount));
        executor.execute();
        executor.commitWorldState();
    }

    //TODO: Isto não é necessário ir a consensus. Vale a pena ter?
    // public BigInteger balanceOf(Address account) {
    //     String encodedAccount = ContractUtils.padAddressTo256Bit(account.toHexString());
        
    //     executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("balanceOf") + encodedAccount));
    //     executor.execute();
        
    //     return ContractUtils.extractIntegerFromReturnData(output);
    // }



}
