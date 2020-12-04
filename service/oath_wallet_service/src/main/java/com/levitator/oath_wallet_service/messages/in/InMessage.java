package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.messages.common.SessionMessage;

public abstract class InMessage extends SessionMessage{        
    public abstract void process(Service serv) throws Exception;    
}
