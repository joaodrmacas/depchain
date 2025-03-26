package pt.tecnico.ulisboa;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import pt.tecnico.ulisboa.contracts.BlacklistContract;
import pt.tecnico.ulisboa.contracts.ISTCoinContract;


public class ContractTests {
    private SimpleWorld world;
    private Address owner;
    private Address userA;
    private Address userB;
    private ISTCoinContract coinContract;
    private BlacklistContract blacklistContract;

    @Before
    public void setup() {
        // Create a new SimpleWorld for each test
        world = new SimpleWorld();
        
        // Create addresses for owner and users
        owner = Address.fromHexString("0x1234567890123456789012345678901234567890");
        userA = Address.fromHexString("0x2345678901234567890123456789012345678901");
        userB = Address.fromHexString("0x3456789012345678901234567890123456789012");
        
        // Initialize contracts
        coinContract = new ISTCoinContract(world, owner);
        blacklistContract = new BlacklistContract(world, owner);
    }

    @Test
    public void testBlacklistContractAddAndRemove() {
        // Test adding to blacklist
        blacklistContract.isBlacklisted(userA);
        blacklistContract.addToBlacklist(userA);
        assertTrue("User A should be blacklisted after addToBlacklist",blacklistContract.isBlacklisted(userA));

        // Test removing from blacklist
        blacklistContract.removeFromBlacklist(userA);
        assertFalse("User A should not be blacklisted after removeFromBlacklist", blacklistContract.isBlacklisted(userA));
    }

    // @Test
    // public void testBlacklistPreventsCoinTransfer() {
    //     // Add userA to blacklist
    //     blacklistContract.addToBlacklist(userA);

    //     // Attempt transfer from blacklisted user should fail
    //     BigInteger transferAmount = BigInteger.valueOf(100);
        
    //     // These operations should throw an exception due to blacklisting
    //     assertThrows(RuntimeException.class, () -> {
    //         coinContract.transfer(userA, userB, transferAmount);
    //     }, "Transfer from blacklisted user should fail");

    //     assertThrows(RuntimeException.class, () -> {
    //         coinContract.transferFrom(owner, userA, userB, transferAmount);
    //     }, "Transfer involving blacklisted user should fail");
    // }

    @Test
    public void testCoinContractTransfer() {
        BigInteger initialAmount = BigInteger.valueOf(1000);
        BigInteger transferAmount = BigInteger.valueOf(100);

        // Simulate an initial coin distribution to userA
        coinContract.transfer(owner, userA, initialAmount);

        // Transfer from userA to userB
        coinContract.transfer(userA, userB, transferAmount);

        // Note: In a real implementation, you'd want to add a method to check balances
        // This test assumes the transfer works without throwing an exception
    }

    @Test
    public void testCoinContractApprove() {
        BigInteger approvalAmount = BigInteger.valueOf(500);

        // Approve userB to spend on behalf of userA
        coinContract.approve(userA, userB, approvalAmount);

        // Attempt transferFrom using the approved amount
        BigInteger transferAmount = BigInteger.valueOf(100);
        coinContract.transferFrom(userB, userA, userB, transferAmount);

        // Note: This test checks that the approve and transferFrom methods work without exceptions
    }

    // @Test
    // public void testBlacklistPreventApprove() {
    //     // Blacklist userA
    //     blacklistContract.addToBlacklist(userA);

    //     BigInteger approvalAmount = BigInteger.valueOf(500);

    //     // Attempt to approve from a blacklisted user should fail
    //     assertThrows(RuntimeException.class, () -> {
    //         coinContract.approve(userA, userB, approvalAmount);
    //     }, "Approve from blacklisted user should fail");
    // }
}
