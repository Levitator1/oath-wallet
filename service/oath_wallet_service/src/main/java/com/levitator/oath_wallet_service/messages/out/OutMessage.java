
package com.levitator.oath_wallet_service.messages.out;
import com.levitator.oath_wallet_service.messages.common.SessionMessage;

/*
*
* Really just to be symmetrical with InMessage, which actually adds something to the interface
*
*/
public abstract class OutMessage extends SessionMessage{

    public OutMessage(long session){
        super(session);
    }
}

