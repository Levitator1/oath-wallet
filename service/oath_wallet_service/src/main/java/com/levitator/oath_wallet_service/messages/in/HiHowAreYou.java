package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Service;

/**
 *
 * A no-op message that the browser can send to ensure that the
 * service/gui process is up
 *
 */
public class HiHowAreYou extends InMessage{

    @Override
    public void process(Service serv) throws Exception {        
    }
            
    @Override
    public String type() {
        return "hello";
    }
}
