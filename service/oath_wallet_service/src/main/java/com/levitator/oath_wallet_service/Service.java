package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.ui.SystemTrayUI;

public class Service {
    
    private SystemTrayUI system_tray_ui;
    
    public Service(String[] args) throws Exception{        
        system_tray_ui = new SystemTrayUI();                                      
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
