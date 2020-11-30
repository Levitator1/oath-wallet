package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.ui.FXApp;
import javafx.application.Application;
import javafx.application.Platform;

public final class Main{
    
    static private int exit_code=0;
    
    //Seems like JFX wants to live on its own thread, so we bootstrap the app
    //from the JFX framework. Also, it seems like if you derive the Main class
    //from Application, then the runtime spuriously complains that FX components are missing
    //Maybe this is not the intended usage, but it would be nice if the error message were relevant.
    public static void main(String[] args) throws Exception {        
        Application.launch(FXApp.class, args);
        System.exit(exit_code);
    }
    
    public static void exit(int result){
        exit_code = result;
        Platform.exit();
    }
    
}
