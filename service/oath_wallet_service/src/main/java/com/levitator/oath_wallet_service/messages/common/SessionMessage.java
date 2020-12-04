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
public abstract class SessionMessage implements TypedMessage {
        
    private long m_session_id;        
    
    public SessionMessage(){
        m_session_id = 0;
    }
    
    public SessionMessage(long id){
        m_session_id = id;
    }
    
    public long session_id(){
        return m_session_id;
    }
    
    //public String type();

    @Override
    public JsonObjectBuilder toJson(){
        var builder = TypedMessage.super.toJson();
        builder.add("session_id", session_id());
        return builder;
    }
    
    @Override
    public void parse(JsonParser parser) {
        m_session_id = Parser.demand_long_named(parser, "session_id");
    }
}
