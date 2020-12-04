package com.levitator.oath_wallet_service.messages.in.internal;
import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.messages.in.InMessage;

//A message the input thread sends the service to express that it is dying
public class FatalMessage extends InMessage{

    @Override
    public void process(Service serv) throws Exception {        
    }

    @Override
    public String type() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
