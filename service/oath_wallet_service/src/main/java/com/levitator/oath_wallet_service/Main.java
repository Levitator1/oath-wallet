package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.ui.FXApp;
import javafx.application.Application;

public final class Main{
    
    static public int exit_code=0;
    
    //Seems like JFX wants to live on its own thread, so we bootstrap the app
    //from the JFX framework.
    public static void main(String[] args) throws Exception {        
        Application.launch(FXApp.class, args);
        System.exit(exit_code);
    }
}
