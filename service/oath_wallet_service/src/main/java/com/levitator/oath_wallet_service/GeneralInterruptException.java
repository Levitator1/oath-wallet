package com.levitator.oath_wallet_service;
import java.nio.channels.FileLockInterruptionException;

//A common exception to wrap InterruptException or FileLockInterruptionException
//as a single concept
public class GeneralInterruptException extends Exception {
    
    public GeneralInterruptException(InterruptedException ex){
        super("Interrupted", ex);
    }
    
    public GeneralInterruptException(FileLockInterruptionException ex){
        super("Interrupted", ex);
    }
    
}
