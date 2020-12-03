package com.levitator.oath_wallet_service.messages.common;

import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;

public interface IMessage {
        
    // Assumes that object start has been picked up already
    public void parse(JsonParser parser);    
    public JsonObjectBuilder toJson();
}
