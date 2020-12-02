package com.levitator.oath_wallet_service;

import javafx.application.Platform;

public final class Main{
    
    static private int exit_code=0;
    //static private Service m_service;

    //It seems like if you derive the Main class
    //from Application, then the runtime spuriously complains that FX components are missing
    //Maybe this is not the intended usage, but it would be nice if the error message were relevant.
    public static void main(String[] args) throws Exception {        
        //m_service = new Service();
        
        try{
            Service.instance.run(args);            
        }
        catch(Exception ex){            
            Service.fatal("Unexpected error. Exiting.", ex);            
        }
        System.exit(exit_code);
    }

    public static void exit(int result){
        exit_code = result;
        Platform.exit();
    }

}
