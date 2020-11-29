package com.levitator.oath_wallet_service.util;

public class GlobParsingException extends Exception {
    public GlobParsingException(String msg, Throwable cause){
        super(msg, cause);
    }
    
    public GlobParsingException(String msg){
        super(msg);
    }
}
