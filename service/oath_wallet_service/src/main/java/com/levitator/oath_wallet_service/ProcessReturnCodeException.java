package com.levitator.oath_wallet_service;

public class ProcessReturnCodeException extends Exception{
    
    private int m_code;
    
    public ProcessReturnCodeException(String msg, int code){
        super(msg);
        m_code = code;
    }
    
    public int result(){
        return m_code;
    }    
}
