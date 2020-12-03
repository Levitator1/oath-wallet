package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.messages.common.TypedMessage;

public interface InMessage extends TypedMessage{
    public void process(Service serv) throws Exception;    
}
