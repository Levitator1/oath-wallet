package com.levitator.oath_wallet_service;

// Thrown when the DomainMapper's configuration is locked indicating that another
// process is already the daemon and front-end
public class LockException extends Exception{
    
    public LockException(){        
    }
    
}
