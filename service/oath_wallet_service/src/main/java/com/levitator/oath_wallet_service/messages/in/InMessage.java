package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.messages.common.TypedMessage;
import java.io.IOException;

public interface InMessage extends TypedMessage{
    public abstract void process(Service serv) throws IOException, InterruptedException;
}
