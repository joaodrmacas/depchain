package pt.tecnico.ulisboa;

import java.time.LocalDateTime;

import pt.tecnico.ulisboa.protocol.ClientReq;
import pt.tecnico.ulisboa.protocol.ClientReq.ClientReqType;
import pt.tecnico.ulisboa.protocol.ClientResp;
import pt.tecnico.ulisboa.utils.types.Logger;

public class Bloc<T> {
    public ClientResp handleDecidedValue(T value) {
        // cast the valuse to ClientReq
        boolean success = false;
        if (value != null) {
            success = true;
            // Add to blockchain
            Logger.LOG("Decided value: " + value);
        }

        LocalDateTime timestamp = LocalDateTime.now();

        // Send answer to clients
        ClientReq decided = (ClientReq) value;

        ClientReqType decidedType = decided.getReqType();

        switch (decidedType) {
            case TRANSFER:
                // call the contract function with the client address

                break;
            case TRANSFER_FROM:
                break;
            case BLACKLIST:
                break;
            case APPROVE:
                break;
            default:
                System.out.println("Invalid request type");
                break;
        }
        return response;

    }

}
