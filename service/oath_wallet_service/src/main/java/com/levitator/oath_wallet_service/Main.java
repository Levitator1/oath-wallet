package com.levitator.oath_wallet_service;

import javafx.application.Platform;

public final class Main{
    
    //Nullable so that we can discern unexpected exits, where no result code was set   
    static private Integer m_exit_code=null;
    
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
            exit_code(-1);
            Service.fatal("Unexpected error. Exiting.", ex);            
        }
        System.exit( exit_code() != null ? m_exit_code : -1);
    }

    public static void exit_code(int result){
        m_exit_code = result;        
    }
    
    public static Integer exit_code(){
        return m_exit_code;
    }
    
}
