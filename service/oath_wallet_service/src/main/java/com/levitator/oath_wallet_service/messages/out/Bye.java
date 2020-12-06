package com.levitator.oath_wallet_service.messages.out;

import com.levitator.oath_wallet_service.messages.out.OutMessage;
import java.io.EOFException;
import javax.json.stream.JsonParser;

public class Bye implements OutMessage {

    static public class ClientSaidBye extends EOFException{}
    
    public Bye(){       
    }
    
    @Override
    public void parse(JsonParser parser) {
        //No fields to parse (other than type)
    }           
        
    //Accepting this server-side turns out to be kind of pointless since
    //we are already looking for end-of-array to decide to close the connection
    //and we still have trouble with that because pipes are seemingly stubborn to close
    /*
    @Override
    public void process(Service serv) throws IOException, InterruptedException {
        throw new ClientSaidBye();
    }
    */

    @Override
    public String type() {
        return "bye";
    }
}
