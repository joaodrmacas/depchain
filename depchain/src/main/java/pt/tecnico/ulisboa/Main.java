package pt.tecnico.ulisboa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.tecnico.ulisboa.utils.ContractUtils;

import org.apache.tuweni.bytes.Bytes;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.*;
import org.hyperledger.besu.evm.fluent.EVMExecutor;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;


public class Main {

    public static Address generateContractAddress(Address deployerAddress) {
        return Address.fromHexString(
            org.web3j.crypto.Hash.sha3String(
                deployerAddress.toHexString() + 
                System.currentTimeMillis()
            ).substring(0, 42)
        );
    }
    
    public static void main(String[] args) {
        String BYTECODE = "608060405234801561000f575f80fd5b5060043610610060575f3560e01c806344337ea114610064578063537df3b614610080578063715018a61461009c5780638da5cb5b146100a6578063f2fde38b146100c4578063fe575a87146100e0575b5f80fd5b61007e60048036038101906100799190610700565b610110565b005b61009a60048036038101906100959190610700565b6102aa565b005b6100a4610443565b005b6100ae610456565b6040516100bb919061073a565b60405180910390f35b6100de60048036038101906100d99190610700565b61047d565b005b6100fa60048036038101906100f59190610700565b610501565b604051610107919061076d565b60405180910390f35b610118610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610186576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161017d906107e0565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff1615610210576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161020790610848565b60405180910390fd5b6001805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167ff9b68063b051b82957fa193585681240904fed808db8b30fc5a2d2202c6ed62760405160405180910390a250565b6102b2610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610320576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610317906107e0565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff166103a9576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103a0906108b0565b60405180910390fd5b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167f2b6bf71b58b3583add364b3d9060ebf8019650f65f5be35f5464b9cb3e4ba2d460405160405180910390a250565b61044b610553565b6104545f6105da565b565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b610485610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036104f5575f6040517f1e4fbdf70000000000000000000000000000000000000000000000000000000081526004016104ec919061073a565b60405180910390fd5b6104fe816105da565b50565b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b61055b61069b565b73ffffffffffffffffffffffffffffffffffffffff16610579610456565b73ffffffffffffffffffffffffffffffffffffffff16146105d85761059c61069b565b6040517f118cdaa70000000000000000000000000000000000000000000000000000000081526004016105cf919061073a565b60405180910390fd5b565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050815f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b5f33905090565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6106cf826106a6565b9050919050565b6106df816106c5565b81146106e9575f80fd5b50565b5f813590506106fa816106d6565b92915050565b5f60208284031215610715576107146106a2565b5b5f610722848285016106ec565b91505092915050565b610734816106c5565b82525050565b5f60208201905061074d5f83018461072b565b92915050565b5f8115159050919050565b61076781610753565b82525050565b5f6020820190506107805f83018461075e565b92915050565b5f82825260208201905092915050565b7f496e76616c6964206164647265737300000000000000000000000000000000005f82015250565b5f6107ca600f83610786565b91506107d582610796565b602082019050919050565b5f6020820190508181035f8301526107f7816107be565b9050919050565b7f4164647265737320616c726561647920626c61636b6c697374656400000000005f82015250565b5f610832601b83610786565b915061083d826107fe565b602082019050919050565b5f6020820190508181035f83015261085f81610826565b9050919050565b7f41646472657373206e6f7420626c61636b6c69737465640000000000000000005f82015250565b5f61089a601783610786565b91506108a582610866565b602082019050919050565b5f6020820190508181035f8301526108c78161088e565b905091905056fea26469706673582212204f0989f48eed08aa21ee184bbca786dcf07b8e4738973d58136087faf980b8b964736f6c634300081a0033";
        String DEPLOYCODE = "608060405234801561000f575f80fd5b50335f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610081575f6040517f1e4fbdf70000000000000000000000000000000000000000000000000000000081526004016100789190610196565b60405180910390fd5b6100908161009660201b60201c565b506101af565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050815f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61018082610157565b9050919050565b61019081610176565b82525050565b5f6020820190506101a95f830184610187565b92915050565b610904806101bc5f395ff3fe608060405234801561000f575f80fd5b5060043610610060575f3560e01c806344337ea114610064578063537df3b614610080578063715018a61461009c5780638da5cb5b146100a6578063f2fde38b146100c4578063fe575a87146100e0575b5f80fd5b61007e60048036038101906100799190610700565b610110565b005b61009a60048036038101906100959190610700565b6102aa565b005b6100a4610443565b005b6100ae610456565b6040516100bb919061073a565b60405180910390f35b6100de60048036038101906100d99190610700565b61047d565b005b6100fa60048036038101906100f59190610700565b610501565b604051610107919061076d565b60405180910390f35b610118610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610186576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161017d906107e0565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff1615610210576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161020790610848565b60405180910390fd5b6001805f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167ff9b68063b051b82957fa193585681240904fed808db8b30fc5a2d2202c6ed62760405160405180910390a250565b6102b2610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1603610320576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610317906107e0565b60405180910390fd5b60015f8273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff166103a9576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016103a0906108b0565b60405180910390fd5b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055508073ffffffffffffffffffffffffffffffffffffffff167f2b6bf71b58b3583add364b3d9060ebf8019650f65f5be35f5464b9cb3e4ba2d460405160405180910390a250565b61044b610553565b6104545f6105da565b565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b610485610553565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036104f5575f6040517f1e4fbdf70000000000000000000000000000000000000000000000000000000081526004016104ec919061073a565b60405180910390fd5b6104fe816105da565b50565b5f60015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff169050919050565b61055b61069b565b73ffffffffffffffffffffffffffffffffffffffff16610579610456565b73ffffffffffffffffffffffffffffffffffffffff16146105d85761059c61069b565b6040517f118cdaa70000000000000000000000000000000000000000000000000000000081526004016105cf919061073a565b60405180910390fd5b565b5f805f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050815f806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b5f33905090565b5f80fd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f6106cf826106a6565b9050919050565b6106df816106c5565b81146106e9575f80fd5b50565b5f813590506106fa816106d6565b92915050565b5f60208284031215610715576107146106a2565b5b5f610722848285016106ec565b91505092915050565b610734816106c5565b82525050565b5f60208201905061074d5f83018461072b565b92915050565b5f8115159050919050565b61076781610753565b82525050565b5f6020820190506107805f83018461075e565b92915050565b5f82825260208201905092915050565b7f496e76616c6964206164647265737300000000000000000000000000000000005f82015250565b5f6107ca600f83610786565b91506107d582610796565b602082019050919050565b5f6020820190508181035f8301526107f7816107be565b9050919050565b7f4164647265737320616c726561647920626c61636b6c697374656400000000005f82015250565b5f610832601b83610786565b915061083d826107fe565b602082019050919050565b5f6020820190508181035f83015261085f81610826565b9050919050565b7f41646472657373206e6f7420626c61636b6c69737465640000000000000000005f82015250565b5f61089a601783610786565b91506108a582610866565b602082019050919050565b5f6020820190508181035f8301526108c78161088e565b905091905056fea26469706673582212204f0989f48eed08aa21ee184bbca786dcf07b8e4738973d58136087faf980b8b964736f6c634300081a0033";
        try {
            // Setup logging
            System.out.println("=== DEBUG: Contract Deployment and Method Calls ===");
    
            SimpleWorld simpleWorld = new SimpleWorld();
    
            // Sender Account Setup
            String paddedSenderHex = "0x4B20993Bc481177ec7E8f571ceCaE8A9e22C02db";
            
            Address senderAddress = Address.fromHexString(paddedSenderHex);
            simpleWorld.createAccount(senderAddress, 0, Wei.fromEth(100));
            
            // Contract Address Setup
            Address contractAddress = generateContractAddress(senderAddress);
            simpleWorld.createAccount(contractAddress, 0, Wei.fromEth(0));
    
            // Prepare Executor
            var executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(byteArrayOutputStream);
            StandardJsonTracer tracer = new StandardJsonTracer(printStream, true, true, true, true);
            executor.tracer(tracer);
    
            System.out.println("\n--- Deploying Contract ---");
            executor.code(Bytes.fromHexString(DEPLOYCODE));
            executor.sender(senderAddress);
            executor.receiver(contractAddress);
            executor.callData(Bytes.EMPTY);
            executor.execute();
            
            executor.code(Bytes.fromHexString(BYTECODE));
            executor.callData(Bytes.fromHexString("8da5cb5b"));
            executor.execute();
            String res = ContractUtils.extractStringFromReturnData(byteArrayOutputStream);
            System.out.println("Owner: " + res);
            //TODO: perguntar para que isto serve e pq e que se fode tudo quando uso
            // WorldUpdater updater = simpleWorld.updater();
            // executor.worldUpdater(updater);
            // updater.commit();
            

            Address blacklistedAddr = Address.fromHexString("1111111111111111111111111111");
            
            // Call isBlacklisted
            executor.callData(Bytes.fromHexString(ContractUtils.getFunctionSelector("isBlacklisted(address)") + ContractUtils.padAddressTo256Bit(blacklistedAddr)));
            executor.execute();
            boolean isBlacklisted = ContractUtils.extractBooleanFromReturnData(byteArrayOutputStream);
            System.out.println("Is Blacklisted: " + isBlacklisted);

            executor.callData(Bytes.fromHexString(ContractUtils.getFunctionSelector("addToBlacklist(address)") + ContractUtils.padAddressTo256Bit(blacklistedAddr)));
            executor.execute();
            System.out.println("Added to the blacklist");

            executor.callData(Bytes.fromHexString(ContractUtils.getFunctionSelector("isBlacklisted(address)") + ContractUtils.padAddressTo256Bit(blacklistedAddr)));
            executor.execute();
            isBlacklisted = ContractUtils.extractBooleanFromReturnData(byteArrayOutputStream);
            System.out.println("Is Blacklisted: " + isBlacklisted);

            executor.callData(Bytes.fromHexString(ContractUtils.getFunctionSelector("removeFromBlacklist(address)") + ContractUtils.padAddressTo256Bit(blacklistedAddr)));
            executor.execute();
            System.out.println("Removed from blacklist");

            executor.callData(Bytes.fromHexString(ContractUtils.getFunctionSelector("isBlacklisted(address)") + ContractUtils.padAddressTo256Bit(blacklistedAddr)));
            executor.execute();
            isBlacklisted = ContractUtils.extractBooleanFromReturnData(byteArrayOutputStream);
            System.out.println("Is Blacklisted: " + isBlacklisted);


            // try {
            //     executor.execute();
            //     executor.worldUpdater(simpleWorld.updater());
            //     executor.commitWorldState();
            //     System.out.println("Contract Deployment Successful");
            // } catch (Exception e) {
            //     System.err.println("Contract Deployment Failed: " + e.getMessage());
            //     e.printStackTrace();
            //     return;
            // }

            try {
                // executor.code(Bytes.fromHexString(BYTECODE));
                // executor.worldUpdater(simpleWorld.updater());
        //     executor.commitWorldState();
                // debugMethodCall(executor, simpleWorld, "owner", 
                // Bytes.fromHexString("8da5cb5b"), 
                // byteArrayOutputStream, senderAddress);
            } catch (Exception e) {
                System.err.println("Unexpected Error: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Unexpected Error: " + e.getMessage());
            e.printStackTrace();
        }



            
    
    
        //     // Step 2: Set Contract Runtime Bytecode
        //     System.out.println("\n--- Setting Contract Runtime Bytecode ---");
        //     executor.code(Bytes.fromHexString(BYTECODE));
        //     executor.sender(senderAddress);
        //     executor.receiver(contractAddress);
        //     executor.worldUpdater(simpleWorld.updater());
        //     executor.commitWorldState();

        //     debugMethodCall(executor, simpleWorld, "owner", 
        //         Bytes.fromHexString("8da5cb5b"), 
        //         byteArrayOutputStream, senderAddress);
    
        //     // Addresses for testing
        //     Address blacklistedAddr = Address.fromHexString("1111111111111111111111111111");
            
        //     // Debug Method Calls
        //     debugMethodCall(executor, simpleWorld, "isBlacklisted", 
        //         Bytes.fromHexString("fe575a87" + padHexStringTo256Bit(blacklistedAddr.toHexString())), 
        //         byteArrayOutputStream, senderAddress);
            
        //     debugMethodCall(executor, simpleWorld, "addToBlacklist", 
        //         Bytes.fromHexString("44337ea1" + padHexStringTo256Bit(blacklistedAddr.toHexString())), 
        //         byteArrayOutputStream, senderAddress);
            
        //     debugMethodCall(executor, simpleWorld, "isBlacklisted", 
        //         Bytes.fromHexString("fe575a87" + padHexStringTo256Bit(blacklistedAddr.toHexString())), 
        //         byteArrayOutputStream, senderAddress);
        // } catch (Exception e) {
        //     System.err.println("Unexpected Error: " + e.getMessage());
        //     e.printStackTrace();
        // }
    }

    

    private static void debugMethodCall(
    EVMExecutor executor, 
    SimpleWorld simpleWorld, 
    String methodName, 
    Bytes callData, 
    ByteArrayOutputStream byteArrayOutputStream,
    Address senderAddress
) {
    System.out.println("\n--- Debugging Method: " + methodName + " ---");
    System.out.println("Sender Address: " + senderAddress);
    System.out.println("Call Data (Hex): " + callData.toHexString());
    
    try {
        executor.sender(senderAddress);
        executor.callData(callData);
        executor.execute();
        
        String tracerOutput = byteArrayOutputStream.toString();
        System.out.println("Full Tracer Output:");
        System.out.println(tracerOutput);
        
        // Additional detailed parsing
        String[] lines = tracerOutput.split("\\r?\\n");
        JsonObject lastJsonTrace = JsonParser.parseString(lines[lines.length - 1]).getAsJsonObject();
        
        System.out.println("\nDetailed Trace Analysis:");
        System.out.println("Program Counter: " + lastJsonTrace.get("pc"));
        System.out.println("Operation: " + lastJsonTrace.get("opName"));
        System.out.println("Remaining Gas: " + lastJsonTrace.get("gas"));
        
        System.out.println("\nStack Contents:");
        JsonArray stackArray = lastJsonTrace.get("stack").getAsJsonArray();
        for (int i = 0; i < stackArray.size(); i++) {
            System.out.println("  Stack[" + i + "]: " + stackArray.get(i).getAsString());
        }
        
    } catch (Exception e) {
        System.err.println("Method Call Failed: " + e.getMessage());
        e.printStackTrace();
    }
}

}