package pt.tecnico.ulisboa.server;

import org.hyperledger.besu.datatypes.Address;

public class Account {
    private int balance;
    private Address address;

    public Account(Address address, int balance) {
        this.balance = balance;
        this.address = address;
    }

    public int getBalance() {
        return balance;
    }

    public Address getAddress() {
        return address;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}