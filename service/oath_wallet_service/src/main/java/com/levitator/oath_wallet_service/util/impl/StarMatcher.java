package com.levitator.oath_wallet_service.util.impl;

import java.nio.CharBuffer;

public class StarMatcher implements StringMatcher{

    CharBuffer buffer;
    
    @Override
    public boolean push(CharBuffer str) {
        if(str.remaining() == 0)
            return false;
        else{
            buffer.append(str);
            return true;
        }
    }

    @Override
    public boolean satisfied() {
        return true;
    }

    @Override
    public void clear() {
        buffer.clear();
    }

    @Override
    public CharBuffer get() {
        return buffer;
    }
    
}
