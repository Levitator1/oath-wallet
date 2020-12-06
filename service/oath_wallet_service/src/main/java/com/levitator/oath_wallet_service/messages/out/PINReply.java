package com.levitator.oath_wallet_service.messages.out;

import javax.json.JsonObjectBuilder;


public class PINReply extends OutMessageBase{

    String m_url, m_cred, m_pin;
    
    public PINReply(long session, String url, String cred, String pin) {
        super(session);
        m_url = url;
        m_cred = cred;
        m_pin = pin;
    }  
    
    @Override
    public String type() {
        return "pin_reply";
    }
    
    @Override
    public JsonObjectBuilder toJson(){
        var builder = super.toJson();
        return builder.add("url", m_url).add( "cred", m_cred ).add( "pin", m_pin );        
    }   
}
