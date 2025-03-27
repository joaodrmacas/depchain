package pt.tecnico.ulisboa;

import java.util.List;
import pt.tecnico.ulisboa.protocol.ClientReq;

public class Block {

    private String prevHash;
    private String blockHash;
    private List<ClientReq> transactions;
    private List<Account> accounts; //TODO: isto vai ter que ser uma lista ou um mapa de todas os contratos e users da nossa blockchain

}e