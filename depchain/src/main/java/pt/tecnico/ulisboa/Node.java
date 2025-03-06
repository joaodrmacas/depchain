package pt.tecnico.ulisboa;

import pt.tecnico.ulisboa.consensus.BFTConsensus;
import pt.tecnico.ulisboa.network.AuthenticatedPerfectLinkImpl;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class Node {

    BFTConsensus<String> consensus;
    PrivateKey privateKey;
    List<PublicKey> publicKeys;
    AuthenticatedPerfectLinkImpl authenticatedPerfectLink;

    public static void main(String[] args){

        

    }

    public void setup(){
        
    }
    
}


