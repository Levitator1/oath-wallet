package com.levitator.oath_wallet_service.messages.out;

import javax.json.JsonObjectBuilder;

public class ErrorMessage extends OutMessage{
    
    private String m_message;
    
    public ErrorMessage(String msg, long session){
        super(session);
        m_message = msg;
    }
    
    @Override
    public JsonObjectBuilder toJson(){
        var builder = super.toJson();
        builder.add("message", m_message);
        return builder;
    }
    
    @Override
    public String type() {
        return "error";
    }
    
}
