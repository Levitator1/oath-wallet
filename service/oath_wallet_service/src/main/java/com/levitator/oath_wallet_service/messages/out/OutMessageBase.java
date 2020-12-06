package com.levitator.oath_wallet_service.messages.out;

import com.levitator.oath_wallet_service.messages.common.SessionMessageBase;

public abstract class OutMessageBase extends SessionMessageBase implements OutMessage {

    public OutMessageBase(long session) {
        super(session);
    }

    @Override
    public abstract String type();    
}
