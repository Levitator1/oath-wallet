package com.levitator.oath_wallet_service;

public class Service {
    
    public Service(String[] args){
        
    }
    
    public int run(){
        return 0;
    }
    
    //Log informative messages
    static public void log(String msg, Exception ex){
    
    }
    
    static public void log(String msg){
        log(msg, null);
    }
    
    //Respond to fatal errors
    static public void fatal(String msg, Exception ex){
        
    }
    
    static public void fatal(String msg){
        fatal(msg, null);
    }
    
}
