package com.levitator.oath_wallet_service.messages.out;

public class ErrorMessage extends Notification{
        
    public ErrorMessage(String msg, long session){
        super(msg, session);        
    }        
    
    @Override
    public String type() {
        return "error";
    }    
}
