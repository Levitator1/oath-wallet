package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.messages.common.SessionMessageBase;
import java.io.IOException;
import javax.json.stream.JsonParser;

/**
 *
 * A no-op message that the browser can send to ensure that the
 * service/gui process is up
 *
 */
public class HiHowAreYou implements InMessage{

    public HiHowAreYou(){
        super();
    }
    
    /*
    public HiHowAreYou(long session) {
        super(session);
    }
    */
            
    @Override
    public String type() {
        return "hello";
    }

    @Override
    public void process(Service serv) throws IOException, InterruptedException {
        serv.log("Browser client connect");
    }

    @Override
    public void parse(JsonParser parser) {
        //nop
    }
}
