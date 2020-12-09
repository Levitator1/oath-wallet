package com.levitator.oath_wallet_service;

/**
 *
 * ykman executed, but the result code was non-zero
 *
 */
public class YkmanExitCodeException extends ProcessReturnCodeException{
    
    public YkmanExitCodeException(ProcessReturnCodeException ex){
        super("'ykman' tool returned non-zero, which generally means there was an error.\nExit code: " +
                ex.result() + ". stderr follows:\n" + ex.getMessage() + "------------------------------------- end stderr\n",  ex.result());
    }
}
