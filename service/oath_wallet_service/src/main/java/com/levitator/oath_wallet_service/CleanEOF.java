package com.levitator.oath_wallet_service;

import java.io.EOFException;

/*
*
* We got an EOF, but it occurred between operations, so it's a clean disconnect
*
*/
public class CleanEOF extends EOFException{    
    public CleanEOF(String msg){
        super(msg);
    }
}
