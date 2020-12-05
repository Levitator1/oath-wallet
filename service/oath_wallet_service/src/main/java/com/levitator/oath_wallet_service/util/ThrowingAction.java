package com.levitator.oath_wallet_service.util;

@FunctionalInterface
public interface ThrowingAction<E extends Exception> {    
    public void run() throws E;        
}
