package pt.tecnico.ulisboa;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.evm.fluent.SimpleWorld;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// Import JUnit's assertThrows
import static org.junit.Assert.assertThrows;

import pt.tecnico.ulisboa.contracts.MergedContract;

public class ContractTests {
    private SimpleWorld world;
    private Address owner;
    private Address userA;
    private Address userB;
    private MergedContract mergedContract;

    @Before
    public void setup() {
        // Create a new SimpleWorld for each test
        world = new SimpleWorld();
        
        // Create addresses for owner and users
        owner = Address.fromHexString("0x1234567890123456789012345678901234567890");
        userA = Address.fromHexString("0x2345678901234567890123456789012345678901");
        userB = Address.fromHexString("0x3456789012345678901234567890123456789012");
        
        mergedContract = new MergedContract(world, owner);
    }

    @Test
    public void testBlacklistContractAddAndRemove() {
        // Log detailed information about contract creation
        System.out.println("Owner Address: " + owner);
        System.out.println("Merge Contract Address: " + mergedContract.getAddress());

        assertNotNull("Merged contract address should not be null", mergedContract.getAddress());
        assertFalse("User A should not be blacklisted after addToBlacklist", mergedContract.isBlacklisted(userA));

        // Test adding to blacklist
        mergedContract.addToBlacklist(userA);
        assertTrue("User A should be blacklisted after addToBlacklist", mergedContract.isBlacklisted(userA));

        // Test removing from blacklist
        mergedContract.removeFromBlacklist(userA);
        assertFalse("User A should not be blacklisted after removeFromBlacklist", mergedContract.isBlacklisted(userA));
    }

    @Test
    public void testBlacklistPreventsCoinTransfer() {
        // Add userA to blacklist
        mergedContract.addToBlacklist(userA);

        // Attempt transfer from blacklisted user should fail
        BigInteger transferAmount = BigInteger.valueOf(100);
        
        // These operations should throw a RuntimeException due to blacklisting
        assertThrows("Transfer from blacklisted user should fail", 
            RuntimeException.class, 
            () -> mergedContract.transfer(userA, userB, transferAmount)
        );

        assertThrows("Transfer involving blacklisted user should fail", 
            RuntimeException.class, 
            () -> mergedContract.transferFrom(owner, userA, userB, transferAmount)
        );
    }

    @Test
    public void testCoinContractTransfer() {
        BigInteger initialAmount = BigInteger.valueOf(1000);
        BigInteger transferAmount = BigInteger.valueOf(100);

        mergedContract.balanceOf(owner);

        mergedContract.transfer(owner, userA, initialAmount);

        assertEquals("User A should have the initial amount of coins", 
            initialAmount, mergedContract.balanceOf(userA));

        // Transfer from userA to userB
        mergedContract.transfer(userA, userB, transferAmount);

        assertEquals("User A should have the remaining amount of coins", 
            initialAmount.subtract(transferAmount), mergedContract.balanceOf(userA));
        assertEquals("User B should have the transferred amount of coins", 
            transferAmount, mergedContract.balanceOf(userB));
    }

    @Test
    public void testCoinContractApprove() {
        BigInteger approvalAmount = BigInteger.valueOf(500);
        BigInteger transferAmount = BigInteger.valueOf(100);

        // First, distribute some initial balance to userA
        mergedContract.transfer(owner, userA, BigInteger.valueOf(1000));

        assertEquals("User A should have the initial amount of coins", 
            BigInteger.valueOf(1000), mergedContract.balanceOf(userA));

        // Approve userB to spend on behalf of userA
        mergedContract.approve(userA, userB, approvalAmount);

        assertEquals("User A should have the initial amount of coins", 
            BigInteger.valueOf(1000), mergedContract.balanceOf(userA));
        assertEquals("User B should have the approved amount of coins", 
            BigInteger.valueOf(500), mergedContract.allowance(userA, userB));

        // Attempt transferFrom using the approved amount
        mergedContract.transferFrom(userB, userA, userB, transferAmount);

        
    }

    @Test
    public void testBlacklistPreventApprove() {
        // Blacklist userA
        mergedContract.addToBlacklist(userA);

        BigInteger approvalAmount = BigInteger.valueOf(500);

        // Attempt to approve from a blacklisted user should fail
        assertThrows("Approve from blacklisted user should fail", 
            RuntimeException.class, 
            () -> mergedContract.approve(userA, userB, approvalAmount)
        );
    }
}