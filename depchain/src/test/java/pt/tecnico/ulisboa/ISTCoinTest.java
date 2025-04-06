// package pt.tecnico.ulisboa;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;

// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertFalse;
// import static org.junit.Assert.assertTrue;
// import static org.junit.Assert.fail;

// import org.hyperledger.besu.evm.fluent.EVMExecutor;
// import org.hyperledger.besu.evm.fluent.SimpleWorld;
// import org.hyperledger.besu.evm.EvmSpecVersion;
// import org.hyperledger.besu.datatypes.Address;
// import org.hyperledger.besu.datatypes.Wei;

// import org.hyperledger.besu.evm.fluent.SimpleAccount;
// import org.hyperledger.besu.evm.worldstate.WorldUpdater;
// import org.hyperledger.besu.evm.account.MutableAccount;

// import org.apache.tuweni.bytes.Bytes;
// import java.math.BigInteger;
// import java.io.ByteArrayOutputStream;
// import java.io.PrintStream;
// import org.hyperledger.besu.evm.tracing.StandardJsonTracer;

// public class ISTCoinTest {
//         private ByteArrayOutputStream output;
//         private EVMExecutor executor;
//         private SimpleWorld world;
//         private Address owner;
//         private Address user1;
//         private Address user2;
//         private Address blacklistedUser;
//         private Address contractAddress;

//         // Contract bytecode and method signatures
//         private static final Bytes CONTRACT_BYTECODE = Bytes.fromHexString("YOUR_CONTRACT_DEPLOYED_BYTECODE");

//         // Function signatures
//         private static final Bytes TRANSFER_SIG = Bytes.fromHexString("0xa9059cbb"); // transfer(address,uint256)
//         private static final Bytes TRANSFER_FROM_SIG = Bytes.fromHexString("0x23b872dd"); // transferFrom(address,address,uint256)
//         private static final Bytes APPROVE_SIG = Bytes.fromHexString("0x095ea7b3"); // approve(address,uint256)
//         private static final Bytes BALANCE_OF_SIG = Bytes.fromHexString("0x70a08231"); // balanceOf(address)
//         private static final Bytes BUY_SIG = Bytes.fromHexString("0xa6f2ae3a"); // buy()
//         private static final Bytes DECIMALS_SIG = Bytes.fromHexString("0x313ce567"); // decimals()
//         private static final Bytes NAME_SIG = Bytes.fromHexString("0x06fdde03"); // name()
//         private static final Bytes SYMBOL_SIG = Bytes.fromHexString("0x95d89b41"); // symbol()
//         private static final Bytes ADD_TO_BLACKLIST_SIG = Bytes.fromHexString("0x44d6808a"); // addToBlacklist(address)
//         private static final Bytes REMOVE_FROM_BLACKLIST_SIG = Bytes.fromHexString("0x1a0e909b"); // removeFromBlacklist(address)
//         private static final Bytes IS_BLACKLISTED_SIG = Bytes.fromHexString("0xfe575a87"); // isBlacklisted(address)
//         private static final Bytes TOKEN_PRICE_SIG = Bytes.fromHexString("0x7d8874f4"); // tokenPricePerEth()

//         @Before
//         public void setup() {
//                 // Initialize the EVM executor and world
//                 world = new SimpleWorld();
//                 output = new ByteArrayOutputStream();
//                 executor = EVMExecutor.evm(EvmSpecVersion.CANCUN);
//                 executor.tracer(new StandardJsonTracer(new PrintStream(output), true, true, true, true));

//                 // Setup test accounts
//                 owner = Address.fromHexString("0x1000000000000000000000000000000000000000");
//                 user1 = Address.fromHexString("0x2000000000000000000000000000000000000000");
//                 user2 = Address.fromHexString("0x3000000000000000000000000000000000000000");
//                 blacklistedUser = Address.fromHexString("0x4000000000000000000000000000000000000000");
//                 contractAddress = Address.fromHexString("0x5000000000000000000000000000000000000000");

//                 // Fund accounts
//                 WorldUpdater worldUpdater = world.updater();
//                 MutableAccount ownerAccount = worldUpdater.getOrCreate(owner);
//                 ownerAccount.setBalance(Wei.of(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(18)))); // 100 ETH

//                 MutableAccount user1Account = worldUpdater.getOrCreate(user1);
//                 user1Account.setBalance(Wei.of(BigInteger.valueOf(10).multiply(BigInteger.TEN.pow(18)))); // 10 ETH

//                 MutableAccount user2Account = worldUpdater.getOrCreate(user2);
//                 user2Account.setBalance(Wei.of(BigInteger.valueOf(10).multiply(BigInteger.TEN.pow(18)))); // 10 ETH

//                 MutableAccount blacklistedUserAccount = worldUpdater.getOrCreate(blacklistedUser);
//                 blacklistedUserAccount.setBalance(Wei.of(BigInteger.valueOf(10).multiply(BigInteger.TEN.pow(18)))); // 10
//                                                                                                                     // ETH

//                 // Deploy contract
//                 MutableAccount contractAccount = worldUpdater.getOrCreate(contractAddress);
//                 contractAccount.setCode(CONTRACT_BYTECODE);

//                 worldUpdater.commit();
//                 executor.worldUpdater(world.updater());
//         }

//         @Test
//         public void testTokenInitialization() {
//                 // Test decimals
//                 executor.sender(user1);
//                 executor.receiver(contractAddress);
//                 executor.callData(DECIMALS_SIG);
//                 executor.execute();

//                 Bytes returnValue = extractReturnData();
//                 int decimals = new BigInteger(returnValue.toArrayUnsafe()).intValue();
//                 assertEquals("Token should have 2 decimals", 2, decimals);

//                 // Test name (would require parsing the ABI-encoded string return value)
//                 // This is simplified for the test
//                 executor.callData(NAME_SIG);
//                 executor.execute();

//                 // Test symbol (would require parsing the ABI-encoded string return value)
//                 // This is simplified for the test
//                 executor.callData(SYMBOL_SIG);
//                 executor.execute();

//                 // Test initial supply - owner should have 100M tokens (with 2 decimals)
//                 Bytes ownerAddressParam = encodeAddress(owner);
//                 executor.callData(Bytes.concatenate(BALANCE_OF_SIG, ownerAddressParam));
//                 executor.execute();

//                 returnValue = extractReturnData();
//                 BigInteger ownerBalance = new BigInteger(returnValue.toArrayUnsafe());
//                 BigInteger expectedSupply = BigInteger.valueOf(100_000_000).multiply(BigInteger.TEN.pow(2));
//                 assertEquals("Owner should have the initial supply", expectedSupply, ownerBalance);
//         }

//         @Test
//         public void testBuyTokens() {
//                 // Get token price
//                 executor.sender(user1);
//                 executor.receiver(contractAddress);
//                 executor.callData(TOKEN_PRICE_SIG);
//                 executor.execute();

//                 Bytes returnValue = extractReturnData();
//                 BigInteger tokenPrice = new BigInteger(returnValue.toArrayUnsafe());

//                 // Record initial balances
//                 BigInteger initialOwnerTokens = getBalance(owner);
//                 BigInteger initialUser1Tokens = getBalance(user1);
//                 Wei initialOwnerEth = world.getAccount(owner).getBalance();
//                 Wei initialUser1Eth = world.getAccount(user1).getBalance();

//                 // User1 buys tokens with 1 ETH
//                 BigInteger ethToSend = BigInteger.TEN.pow(18); // 1 ETH

//                 executor.sender(user1);
//                 executor.receiver(contractAddress);
//                 executor.callData(BUY_SIG);
//                 executor.ethValue(Wei.of(ethToSend));
//                 executor.execute();

//                 // Check token balances after purchase
//                 BigInteger expectedTokens = tokenPrice.multiply(ethToSend)
//                                 .multiply(BigInteger.TEN.pow(2)) // Account for 2 decimals
//                                 .divide(BigInteger.TEN.pow(18)); // Adjust for ETH decimals

//                 BigInteger finalOwnerTokens = getBalance(owner);
//                 BigInteger finalUser1Tokens = getBalance(user1);

//                 assertEquals("Owner token balance should decrease by tokens sold",
//                                 initialOwnerTokens.subtract(expectedTokens), finalOwnerTokens);

//                 assertEquals("User1 token balance should increase by tokens bought",
//                                 initialUser1Tokens.add(expectedTokens), finalUser1Tokens);

//                 // Check ETH balances after purchase
//                 Wei finalOwnerEth = world.getAccount(owner).getBalance();
//                 Wei finalUser1Eth = world.getAccount(user1).getBalance();

//                 assertEquals("Owner ETH balance should increase by ETH received",
//                                 initialOwnerEth.add(Wei.of(ethToSend)), finalOwnerEth);

//                 assertEquals("User1 ETH balance should decrease by ETH sent",
//                                 initialUser1Eth.subtract(Wei.of(ethToSend)), finalUser1Eth);
//         }

//         @Test
//         public void testTokenTransfer() {
//                 // First, give user1 some tokens (simulating previous purchase)
//                 BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2)); // 1000 tokens with
//                                                                                                      // 2 decimals
//                 transferTokens(owner, user1, tokensForUser);

//                 // Record initial balances
//                 BigInteger initialUser1Balance = getBalance(user1);
//                 BigInteger initialUser2Balance = getBalance(user2);

//                 // Transfer tokens from user1 to user2
//                 BigInteger transferAmount = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(2)); // 500 tokens
//                 transferTokens(user1, user2, transferAmount);

//                 // Check final balances
//                 BigInteger finalUser1Balance = getBalance(user1);
//                 BigInteger finalUser2Balance = getBalance(user2);

//                 assertEquals("User1 balance should decrease by transferred amount",
//                                 initialUser1Balance.subtract(transferAmount), finalUser1Balance);

//                 assertEquals("User2 balance should increase by transferred amount",
//                                 initialUser2Balance.add(transferAmount), finalUser2Balance);
//         }

//         @Test
//         public void testBlacklisting() {
//                 // Give some tokens to blacklistedUser first
//                 BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2));
//                 transferTokens(owner, blacklistedUser, tokensForUser);

//                 // Add user to blacklist
//                 executor.sender(owner);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(ADD_TO_BLACKLIST_SIG, encodeAddress(blacklistedUser)));
//                 executor.execute();

//                 // Check if user is blacklisted
//                 executor.callData(Bytes.concatenate(IS_BLACKLISTED_SIG, encodeAddress(blacklistedUser)));
//                 executor.execute();

//                 Bytes returnValue = extractReturnData();
//                 boolean isBlacklisted = !returnValue.isZero();
//                 assertTrue("User should be blacklisted", isBlacklisted);

//                 // Try to transfer tokens from blacklisted user (should fail)
//                 // This would require checking for revert conditions which is complex in this
//                 // testing setup

//                 // Remove from blacklist
//                 executor.callData(Bytes.concatenate(REMOVE_FROM_BLACKLIST_SIG, encodeAddress(blacklistedUser)));
//                 executor.execute();

//                 // Check if user is still blacklisted
//                 executor.callData(Bytes.concatenate(IS_BLACKLISTED_SIG, encodeAddress(blacklistedUser)));
//                 executor.execute();

//                 returnValue = extractReturnData();
//                 isBlacklisted = !returnValue.isZero();
//                 assertFalse("User should no longer be blacklisted", isBlacklisted);
//         }

//         @Test
//         public void testTransferFromBlacklistedAddressFails() {
//                 // Give tokens to blacklistedUser
//                 BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2));
//                 transferTokens(owner, blacklistedUser, tokensForUser);

//                 // Add user to blacklist
//                 executor.sender(owner);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(ADD_TO_BLACKLIST_SIG, encodeAddress(blacklistedUser)));
//                 executor.execute();

//                 // Record initial balances
//                 BigInteger initialBlacklistedBalance = getBalance(blacklistedUser);
//                 BigInteger initialUser2Balance = getBalance(user2);

//                 // Try to transfer from blacklisted address (should revert)
//                 // In real testing environment, we would check for revert status
//                 executor.sender(blacklistedUser);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(
//                                 TRANSFER_SIG,
//                                 encodeAddress(user2),
//                                 encodeUint256(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(2)))));
//                 executor.execute();

//                 // Balances should remain unchanged
//                 BigInteger finalBlacklistedBalance = getBalance(blacklistedUser);
//                 BigInteger finalUser2Balance = getBalance(user2);

//                 assertEquals("Blacklisted user balance should remain unchanged after failed transfer",
//                                 initialBlacklistedBalance, finalBlacklistedBalance);

//                 assertEquals("Recipient balance should remain unchanged after failed transfer",
//                                 initialUser2Balance, finalUser2Balance);
//         }

//         @Test
//         public void testTransferToBlacklistedAddressFails() {
//                 // Give tokens to user1
//                 BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2));
//                 transferTokens(owner, user1, tokensForUser);

//                 // Add blacklistedUser to blacklist
//                 executor.sender(owner);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(ADD_TO_BLACKLIST_SIG, encodeAddress(blacklistedUser)));
//                 executor.execute();

//                 // Record initial balances
//                 BigInteger initialUser1Balance = getBalance(user1);
//                 BigInteger initialBlacklistedBalance = getBalance(blacklistedUser);

//                 // Try to transfer to blacklisted address (should revert)
//                 executor.sender(user1);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(
//                                 TRANSFER_SIG,
//                                 encodeAddress(blacklistedUser),
//                                 encodeUint256(BigInteger.valueOf(100).multiply(BigInteger.TEN.pow(2)))));
//                 executor.execute();

//                 // Balances should remain unchanged
//                 BigInteger finalUser1Balance = getBalance(user1);
//                 BigInteger finalBlacklistedBalance = getBalance(blacklistedUser);

//                 assertEquals("Sender balance should remain unchanged after failed transfer",
//                                 initialUser1Balance, finalUser1Balance);

//                 assertEquals("Blacklisted user balance should remain unchanged after failed transfer",
//                                 initialBlacklistedBalance, finalBlacklistedBalance);
//         }

//         @Test
//         public void testApproveAndTransferFromWithBlacklist() {
//                 // Give tokens to user1
//                 BigInteger tokensForUser = BigInteger.valueOf(1000).multiply(BigInteger.TEN.pow(2));
//                 transferTokens(owner, user1, tokensForUser);

//                 // User1 approves user2 to spend tokens
//                 BigInteger approveAmount = BigInteger.valueOf(500).multiply(BigInteger.TEN.pow(2));
//                 executor.sender(user1);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(
//                                 APPROVE_SIG,
//                                 encodeAddress(user2),
//                                 encodeUint256(approveAmount)));
//                 executor.execute();

//                 // Record initial balances
//                 BigInteger initialUser1Balance = getBalance(user1);
//                 BigInteger initialUser2Balance = getBalance(user2);

//                 // User2 transfers tokens from user1 to themselves
//                 BigInteger transferAmount = BigInteger.valueOf(300).multiply(BigInteger.TEN.pow(2));
//                 executor.sender(user2);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(
//                                 TRANSFER_FROM_SIG,
//                                 encodeAddress(user1),
//                                 encodeAddress(user2),
//                                 encodeUint256(transferAmount)));
//                 executor.execute();

//                 // Check final balances
//                 BigInteger finalUser1Balance = getBalance(user1);
//                 BigInteger finalUser2Balance = getBalance(user2);

//                 assertEquals("User1 balance should decrease by transferred amount",
//                                 initialUser1Balance.subtract(transferAmount), finalUser1Balance);

//                 assertEquals("User2 balance should increase by transferred amount",
//                                 initialUser2Balance.add(transferAmount), finalUser2Balance);

//                 // Now blacklist user2 and try again
//                 executor.sender(owner);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(ADD_TO_BLACKLIST_SIG, encodeAddress(user2)));
//                 executor.execute();

//                 // Try transferFrom with blacklisted spender (should fail)
//                 executor.sender(user2);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(
//                                 TRANSFER_FROM_SIG,
//                                 encodeAddress(user1),
//                                 encodeAddress(user2),
//                                 encodeUint256(transferAmount)));
//                 executor.execute();

//                 // Balances should remain unchanged after failed transfer
//                 BigInteger afterBlacklistUser1Balance = getBalance(user1);
//                 BigInteger afterBlacklistUser2Balance = getBalance(user2);

//                 assertEquals("User1 balance should remain unchanged after failed transfer",
//                                 finalUser1Balance, afterBlacklistUser1Balance);

//                 assertEquals("User2 balance should remain unchanged after failed transfer",
//                                 finalUser2Balance, afterBlacklistUser2Balance);
//         }

//         @Test
//         public void testOwnerCannotBuyTokens() {
//                 // Record initial balances
//                 BigInteger initialOwnerTokens = getBalance(owner);
//                 Wei initialOwnerEth = world.getAccount(owner).getBalance();

//                 // Owner tries to buy tokens (should fail)
//                 BigInteger ethToSend = BigInteger.TEN.pow(18); // 1 ETH

//                 executor.sender(owner);
//                 executor.receiver(contractAddress);
//                 executor.callData(BUY_SIG);
//                 executor.ethValue(Wei.of(ethToSend));
//                 executor.execute();

//                 // Balances should remain unchanged
//                 BigInteger finalOwnerTokens = getBalance(owner);
//                 Wei finalOwnerEth = world.getAccount(owner).getBalance();

//                 assertEquals("Owner token balance should remain unchanged",
//                                 initialOwnerTokens, finalOwnerTokens);

//                 assertEquals("Owner ETH balance should remain unchanged",
//                                 initialOwnerEth, finalOwnerEth);
//         }

//         // Helper methods

//         private BigInteger getBalance(Address address) {
//                 executor.sender(user1); // Any sender will work for a read operation
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(BALANCE_OF_SIG, encodeAddress(address)));
//                 executor.execute();

//                 Bytes returnValue = extractReturnData();
//                 return new BigInteger(returnValue.toArrayUnsafe());
//         }

//         private void transferTokens(Address from, Address to, BigInteger amount) {
//                 executor.sender(from);
//                 executor.receiver(contractAddress);
//                 executor.callData(Bytes.concatenate(
//                                 TRANSFER_SIG,
//                                 encodeAddress(to),
//                                 encodeUint256(amount)));
//                 executor.execute();
//         }

//         private Bytes encodeAddress(Address address) {
//                 // Ethereum addresses are 20 bytes but need to be padded to 32 bytes for ABI
//                 // encoding
//                 byte[] paddedAddress = new byte[32];
//                 byte[] addressBytes = address.toArrayUnsafe();
//                 System.arraycopy(addressBytes, 0, paddedAddress, 32 - addressBytes.length, addressBytes.length);
//                 return Bytes.wrap(paddedAddress);
//         }

//         private Bytes encodeUint256(BigInteger value) {
//                 // Encode a uint256 value to a 32-byte array
//                 byte[] valueBytes = value.toByteArray();
//                 byte[] paddedValue = new byte[32];

//                 // Handle potential sign byte
//                 int srcPos = (valueBytes[0] == 0 && valueBytes.length > 1) ? 1 : 0;
//                 int length = Math.min(valueBytes.length - srcPos, 32);

//                 System.arraycopy(valueBytes, srcPos, paddedValue, 32 - length, length);
//                 return Bytes.wrap(paddedValue);
//         }

//         private Bytes extractReturnData() {
//                 // Extract the return data from the output
//                 // Reset the output stream for the next execution
//                 String outputStr = output.toString();
//                 output.reset();

//                 // In a real implementation, you would parse the JSON output
//                 // For this example, we'll simulate return data based on the pattern in the
//                 // second file
//                 // This is a placeholder and would need proper implementation based on your
//                 // exact needs

//                 // Example based on ContractUtils.extractBigIntegerFromReturnData in the second
//                 // file
//                 int returnDataIndex = outputStr.lastIndexOf("returnData");
//                 if (returnDataIndex != -1) {
//                         int valueStartIndex = outputStr.indexOf(":", returnDataIndex);
//                         int valueEndIndex = outputStr.indexOf("}", valueStartIndex);
//                         if (valueStartIndex != -1 && valueEndIndex != -1) {
//                                 String hexValue = outputStr.substring(valueStartIndex + 1, valueEndIndex).trim();
//                                 hexValue = hexValue.replace("\"", "").replace(",", "");
//                                 return Bytes.fromHexString(hexValue);
//                         }
//                 }

//                 return Bytes.EMPTY;
//         }
// }