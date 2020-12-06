package com.levitator.oath_wallet_service.messages.common;

import com.levitator.oath_wallet_service.Parser;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;

/*
*
* A session message is a typed message which is stamped with a browser session identifier
* which, for our purposes with Firefox, means the unique identifier which identifies a browsing tab
*
*/
public interface SessionMessage extends TypedMessage {
                
    public long session_id();
    public void session_id(long v);
    
    @Override
    public default JsonObjectBuilder toJson(){
        var builder = TypedMessage.super.toJson();
        builder.add("session_id", session_id());
        return builder;
    }
    
    @Override
    public default void parse(JsonParser parser) {
        session_id(Parser.demand_long_named(parser, "session_id"));
    }
}
