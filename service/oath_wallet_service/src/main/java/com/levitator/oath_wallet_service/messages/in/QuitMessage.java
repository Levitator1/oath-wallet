package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Service;
import javax.json.stream.JsonParser;


public class QuitMessage implements InMessage{

    @Override
    public void process(Service serv) throws InterruptedException{
        serv.exit(0);
    }

    @Override
    public String type() {
        return "quit";
    }       

    @Override
    public void parse(JsonParser parser) {        
        //no fields to parse
    }
}
