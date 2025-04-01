// package pt.tecnico.ulisboa.contracts;

// import java.math.BigInteger;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.Map;

// import org.apache.tuweni.bytes.Bytes;
// import org.hyperledger.besu.datatypes.Address;
// import org.hyperledger.besu.datatypes.Wei;
// import org.hyperledger.besu.evm.fluent.SimpleWorld;

// import pt.tecnico.ulisboa.utils.ContractUtils;
// import pt.tecnico.ulisboa.utils.types.Logger;

// public class MergedContract extends Contract {

//     private final String BYTECODE;
//     private final String DEPLOYCODE;

//     public MergedContract(Address address, SimpleWorld world) {
//         super(address, world);

//         // TODO: Read from genesis block
//         // Read bytecode and deploy code from respective files
//         try {
//             this.BYTECODE = Files
//                     .readString(Paths.get("src/main/java/pt/tecnico/ulisboa/contracts/MergedContractByteCode.txt"));
//             this.DEPLOYCODE = Files
//                     .readString(Paths.get("src/main/java/pt/tecnico/ulisboa/contracts/MergedContractDeployCode.txt"));
//         } catch (Exception e) {
//             Logger.LOG("Error reading bytecode or deploy code files: " + e.getMessage());
//             throw new RuntimeException("Failed to read bytecode or deploy code files", e);
//         }

//         // Method signatures for contract interactions
//         METHOD_SIGNATURES = Map.of(
//                 "addToBlacklist", ContractUtils.getFunctionSelector("addToBlacklist(address)"),
//                 "removeFromBlacklist", ContractUtils.getFunctionSelector("removeFromBlacklist(address)"),
//                 "isBlacklisted", ContractUtils.getFunctionSelector("isBlacklisted(address)"),
//                 "transfer", ContractUtils.getFunctionSelector("transfer(address,uint256)"),
//                 "transferFrom", ContractUtils.getFunctionSelector("transferFrom(address,address,uint256)"),
//                 "approve", ContractUtils.getFunctionSelector("approve(address,uint256)"),
//                 "balanceOf", ContractUtils.getFunctionSelector("balanceOf(address)"),
//                 "allowance", ContractUtils.getFunctionSelector("allowance(address,address)"),
//                 "buy", ContractUtils.getFunctionSelector("buy()"));

//         deploy();
//     }

//     // Blacklist Management Methods
//     public void addToBlacklist(Address sender, Address addressToBlacklist) {
//         if (addressToBlacklist == null) {
//             throw new IllegalArgumentException("Address cannot be null");
//         }
//         try {
//             String encodedAddress = ContractUtils.padAddressTo256Bit(addressToBlacklist);
//             Bytes callData = Bytes.fromHexString(METHOD_SIGNATURES.get("addToBlacklist") + encodedAddress);
//             executor.sender(sender);
//             executor.callData(callData);
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//             Logger.LOG("Address " + addressToBlacklist + " added to blacklist");
//         } catch (Exception e) {
//             throw new RuntimeException("Failed to add " + addressToBlacklist + " address to blacklist", e);
//         }
//     }

//     public void removeFromBlacklist(Address sender, Address addressToRemove) {
//         try {
//             String encodedAddress = ContractUtils.padAddressTo256Bit(addressToRemove);
//             executor.sender(sender);
//             executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("removeFromBlacklist") + encodedAddress));
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//             Logger.LOG("Address " + addressToRemove + " was removed from blacklist");
//         } catch (Exception e) {
//             Logger.LOG("Error removing address from blacklist: " + e.getMessage());
//             throw new RuntimeException("Failed to remove address from blacklist", e);
//         }
//     }

//     public boolean isBlacklisted(Address sender, Address addressToCheck) {
//         try {
//             String encodedAddress = ContractUtils.padAddressTo256Bit(addressToCheck);
//             executor.sender(sender);
//             executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("isBlacklisted") + encodedAddress));
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//             boolean res = ContractUtils.extractBooleanFromReturnData(this.output);
//             Logger.LOG("Address " + addressToCheck + (res ? " is " : " is not ") + "on the blacklist");
//             return res;
//         } catch (Exception e) {
//             Logger.LOG("Error checking if address is blacklisted: " + e.getMessage());
//             throw new RuntimeException("Failed to check if address is blacklisted", e);
//         }
//     }

//     // Token Transfer Methods
//     public void transfer(Address sender, Address recipient, BigInteger amount) {
//         try {
//             String encodedRecipient = ContractUtils.padAddressTo256Bit(recipient);
//             String encodedAmount = ContractUtils.padBigIntegerTo256Bit(amount);

//             executor.sender(sender);
//             executor.callData(
//                     Bytes.fromHexString(METHOD_SIGNATURES.get("transfer") + encodedRecipient + encodedAmount));
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//         } catch (Exception e) {
//             Logger.LOG("Error transferring tokens: " + e.getMessage());
//             throw new RuntimeException("Failed to transfer tokens", e);
//         }
//     }

//     public void transferFrom(Address sender, Address from, Address to, BigInteger amount) {
//         try {
//             String encodedFrom = ContractUtils.padAddressTo256Bit(from);
//             String encodedTo = ContractUtils.padAddressTo256Bit(to);
//             String encodedAmount = ContractUtils.padBigIntegerTo256Bit(amount);

//             executor.sender(sender);
//             executor.callData(Bytes
//                     .fromHexString(METHOD_SIGNATURES.get("transferFrom") + encodedFrom + encodedTo + encodedAmount));
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//         } catch (Exception e) {
//             Logger.LOG("Error transferring tokens: " + e.getMessage());
//             throw new RuntimeException("Failed to transfer tokens", e);
//         }
//     }

//     public void approve(Address allower, Address allowee, BigInteger amount) {

//         try {
//             String encodedSpender = ContractUtils.padAddressTo256Bit(allowee);
//             String encodedAmount = ContractUtils.padBigIntegerTo256Bit(amount);
//             executor.sender(allower);
//             executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("approve") + encodedSpender + encodedAmount));
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);
//         } catch (Exception e) {
//             Logger.LOG("Error approving tokens: " + e.getMessage());
//             throw new RuntimeException("Failed to approve tokens", e);
//         }

//     }

//     public BigInteger allowance(Address allower, Address allowee) {
//         // TODO: there should only be one argument (the allower)
//         try {
//             String encodedAllower = ContractUtils.padAddressTo256Bit(allower);
//             String encodedAllowee = ContractUtils.padAddressTo256Bit(allowee);

//             executor.sender(allower);
//             executor.callData(
//                     Bytes.fromHexString(METHOD_SIGNATURES.get("allowance") + encodedAllower + encodedAllowee));
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//             BigInteger res = ContractUtils.extractBigIntegerFromReturnData(this.output);
//             Logger.LOG("Allowance of " + allower + " to " + allowee + " is " + res);
//             return res;
//         } catch (Exception e) {
//             Logger.LOG("Error getting allowance: " + e.getMessage());
//             throw new RuntimeException("Failed to get allowance", e);
//         }

//     }

//     public BigInteger balanceOf(Address account) {
//         String encodedAccount = ContractUtils.padAddressTo256Bit(account);

//         executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("balanceOf") + encodedAccount));
//         executor.execute();

//         BigInteger res = ContractUtils.extractBigIntegerFromReturnData(this.output);
//         Logger.LOG("Balance of " + account + " is " + res);
//         return res;
//     }

//     public void buy(Address sender, Wei depAmount, BigInteger tokenAmount) {
//         try {
//             String encodedAmount = ContractUtils.padBigIntegerTo256Bit(tokenAmount);

//             executor.sender(sender);
//             executor.ethValue(depAmount); // TODO: check if this is correct
//             executor.callData(Bytes.fromHexString(METHOD_SIGNATURES.get("buy") + encodedAmount));
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//         } catch (Exception e) {
//             Logger.LOG("Error buying tokens: " + e.getMessage());
//             throw new RuntimeException("Failed to buy tokens", e);
//         }
//     }

//     @Override
//     public void deploy(Object... args) {
//         try {
//             executor.code(Bytes.fromHexString(DEPLOYCODE));
//             executor.callData(Bytes.EMPTY);
//             executor.execute();

//             ContractUtils.checkForExecutionErrors(this.output);

//             // Update to runtime bytecode
//             executor.code(Bytes.fromHexString(BYTECODE));

//             Logger.LOG("Merged contract deployed successfully");
//         } catch (Exception e) {
//             Logger.LOG("Failed to deploy Merged contract: " + e.getMessage());
//             throw new RuntimeException("Deployment failed", e);
//         }
//     }
// }