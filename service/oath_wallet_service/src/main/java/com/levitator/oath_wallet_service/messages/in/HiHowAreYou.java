package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.messages.common.SessionMessageBase;

/**
 *
 * A no-op message that the browser can send to ensure that the
 * service/gui process is up
 *
 */
public class HiHowAreYou extends SessionMessageBase{

    public HiHowAreYou(){
        super();
    }
    
    public HiHowAreYou(long session) {
        super(session);
    }
            
    @Override
    public String type() {
        return "hello";
    }
}
