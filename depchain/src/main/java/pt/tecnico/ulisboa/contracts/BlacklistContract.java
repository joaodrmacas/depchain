package pt.tecnico.ulisboa.contracts;

import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.apache.tuweni.bytes.Bytes;

import pt.tecnico.ulisboa.utils.ContractUtils;

public class BlacklistContract extends Contract {
    
    public BlacklistContract(SimpleWorld world, Address owner) {
        super(world,owner);
        //TODO: fill these
        METHOD_SIGNATURES = Map.of(
            "addToBlacklist", "AAAAA",
            "removeFromBlacklist", "AAAAA",
            "isBlacklisted", "AAAAA"
        );
    }
    

    //TODO: testar
    public void addToBlacklist(Address addressToBlacklist) {
        String encodedAddress = ContractUtils.padAddressTo256Bit(addressToBlacklist.toHexString());
        
        executor.worldUpdater(world.updater());
        executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("addToBlacklist") + encodedAddress));
        executor.execute();
        executor.commitWorldState();
    }

    //TODO: testar
    public boolean isBlacklisted(Address addressToCheck) {
        String encodedAddress = ContractUtils.padAddressTo256Bit(addressToCheck.toHexString());
        
        executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("isBlacklisted") + encodedAddress));
        executor.execute();
        
        return ContractUtils.extractBooleanFromReturnData(output);
    }

    //TODO: testar
    public void removeFromBlacklist(Address addressToRemove) {
        String encodedAddress = ContractUtils.padAddressTo256Bit(addressToRemove.toHexString());
        
        executor.worldUpdater(world.updater());
        executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("removeFromBlacklist") + encodedAddress));
        executor.execute();
        executor.commitWorldState();
    }
}
