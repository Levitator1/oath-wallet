package com.levitator.oath_wallet_service.util.impl;

import java.nio.CharBuffer;

public interface StringMatcher {
    
    //Pushes as much of buf into the matcher as will match
    //Returns true if any matching occurred
    public boolean push(CharBuffer buf);
    
    //Returns true if the matcher's conditions are satisfied
    public boolean satisfied();
    
    //Clear's the matcher's buffer
    public void clear();
    
    //Retrieves the matcher's buffer
    public CharBuffer get();    
}
