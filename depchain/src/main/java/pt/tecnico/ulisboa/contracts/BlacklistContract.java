package pt.tecnico.ulisboa.contracts;

import java.util.Map;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.apache.tuweni.bytes.Bytes;

import pt.tecnico.ulisboa.utils.types.Logger;
import pt.tecnico.ulisboa.utils.ContractUtils;

public class BlacklistContract extends Contract {
    
    public BlacklistContract(SimpleWorld world, Address owner) {
        super(world,owner);
        //TODO: fill these
        METHOD_SIGNATURES = Map.of(
            // "addToBlacklist", ContractUtils.getFunctionSelector("addToBlacklist(address)"),
            // "removeFromBlacklist", ContractUtils.getFunctionSelector("removeFromBlacklist(address)"),
            // "isBlacklisted", ContractUtils.getFunctionSelector("isBlacklisted(address)")
            "addToBlacklist", "44337ea1",
            "removeFromBlacklist","537df3b6",
            "isBlacklisted", "fe575a87"
        );
        setByteCode("608060405234801561000f575f80fd5b5060043610610060575f3560e01c806344337ea114610064578063537df3b614610080578063715018a61461009c5780638da5cb5b146100a6578063f2fde38b146100c4578063fe575a87146100e0575b5f80fd5b61007e60048036038101906100799190610700565b610110565b005b61009a60048036038101906100959190610700565b6102aa565b005b6100a4610443565b005b6100ae610456565b6040516100bb919061073a565b60405180910390f35b6100de60048036038101906100d99190610700565b61047d565b005b6100fa60048036038101906100f59190610700565b610501565b604051610107919061076d565b60405180910390f35b610118610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610186576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161017d906107e0565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff1615610210576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161020790610848565b60405180910390fd5b6001805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167ff9b68063b051b82957fa193585681240904fed808db8b30fc5a2d2202c6ed62760405160405180910390a250565b6102b2610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610320576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610317906107e0565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff166103a9576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103a0906108b0565b60405180910390fd5b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167f2b6bf71b58b3583add364b3d9060ebf8019650f65f5be35f5464b9cb3e4ba2d460405160405180910390a250565b61044b610553565b6104545f6105da565b565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b610485610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036104f5575f6040517f1e4fbdf70000000000000000000000000000000000000000000000000000000081526004016104ec919061073a565b60405180910390fd5b6104fe816105da565b50565b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b61055b61069b565b73ffffffffffffffffffffffffffffffffffffffff16610579610456565b73ffffffffffffffffffffffffffffffffffffffff16146105d85761059c61069b565b6040517f118cdaa70000000000000000000000000000000000000000000000000000000081526004016105cf919061073a565b60405180910390fd5b565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050815f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b5f33905090565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6106cf826106a6565b9050919050565b6106df816106c5565b81146106e9575f80fd5b50565b5f813590506106fa816106d6565b92915050565b5f60208284031215610715576107146106a2565b5b5f610722848285016106ec565b91505092915050565b610734816106c5565b82525050565b5f60208201905061074d5f83018461072b565b92915050565b5f8115159050919050565b61076781610753565b82525050565b5f6020820190506107805f83018461075e565b92915050565b5f82825260208201905092915050565b7f496e76616c6964206164647265737300000000000000000000000000000000005f82015250565b5f6107ca600f83610786565b91506107d582610796565b602082019050919050565b5f6020820190508181035f8301526107f7816107be565b9050919050565b7f4164647265737320616c726561647920626c61636b6c697374656400000000005f82015250565b5f610832601b83610786565b915061083d826107fe565b602082019050919050565b5f6020820190508181035f83015261085f81610826565b9050919050565b7f41646472657373206e6f7420626c61636b6c69737465640000000000000000005f82015250565b5f61089a601783610786565b91506108a582610866565b602082019050919050565b5f6020820190508181035f8301526108c78161088e565b905091905056fea26469706673582212204f0989f48eed08aa21ee184bbca786dcf07b8e4738973d58136087faf980b8b964736f6c634300081a0033");
    }
    

    //TODO: testar
    public void addToBlacklist(Address addressToBlacklist) {
        String encodedAddress = ContractUtils.padAddressTo256Bit(addressToBlacklist.toHexString());
        try {
            output.reset();
            
            executor.worldUpdater(world.updater());
            Bytes callData = Bytes.fromHexString(METHOD_SIGNATURES.get("addToBlacklist") + encodedAddress);
            
            System.out.println("Method Signature: " + METHOD_SIGNATURES.get("addToBlacklist"));
            System.out.println("Encoded Address: " + encodedAddress);
            System.out.println("Full Call Data: " + callData.toHexString());
            
            executor.callData(callData);
            executor.execute();
            executor.commitWorldState();
            
            // Additional logging
            Logger.LOG("Address " + addressToBlacklist + " added to blacklist");
        } catch (Exception e) {
            System.err.println("Full exception in addToBlacklist:");
            e.printStackTrace();
            throw new RuntimeException("Failed to add address to blacklist", e);
        }
    }

    //TODO: testar
    public boolean isBlacklisted(Address addressToCheck) {
        String encodedAddress = ContractUtils.padAddressTo256Bit(addressToCheck.toHexString());
        
        try {
            // Reset output to ensure clean state
            output.reset();
            
            executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("isBlacklisted") + encodedAddress));
            executor.execute();
            
            boolean res = ContractUtils.extractBooleanFromReturnData(output);
            Logger.LOG("Detailed isBlacklisted check for " + addressToCheck + ": " + res);
            return res;
        } catch (Exception e) {
            Logger.LOG("Error checking if address is blacklisted: " + e.getMessage());
            throw new RuntimeException("Failed to check if address is blacklisted", e);
        }
    }

    //TODO: testar
    public void removeFromBlacklist(Address addressToRemove) {
        String encodedAddress = ContractUtils.padAddressTo256Bit(addressToRemove.toHexString());
        
        try {
            // Reset output to ensure clean state
            output.reset();
            
            executor.worldUpdater(world.updater());
            executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("removeFromBlacklist") + encodedAddress));
            executor.execute();
            executor.commitWorldState();
            
            Logger.LOG("Address " + addressToRemove + " removed from blacklist");
            
            // Verify blacklist status immediately after removing
            boolean isBlacklisted = isBlacklisted(addressToRemove);
            if (isBlacklisted) {
                throw new RuntimeException("Failed to remove address from blacklist");
            }
        } catch (Exception e) {
            Logger.LOG("Error removing address from blacklist: " + e.getMessage());
            throw new RuntimeException("Failed to remove address from blacklist", e);
        }
    }
}
