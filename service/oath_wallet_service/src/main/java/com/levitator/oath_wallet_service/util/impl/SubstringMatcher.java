package com.levitator.oath_wallet_service.util.impl;

import com.levitator.oath_wallet_service.util.GlobParsingException;
import java.nio.CharBuffer;
import java.util.Arrays;

public class SubstringMatcher implements StringMatcher{

    private String m_substring;
    int pos=0;
    
    public SubstringMatcher(CharBuffer glob) throws GlobParsingException{
        
        int len=0;
        CharBuffer tmp = CharBuffer.allocate(glob.length());
        
        char ch;
        while(glob.hasRemaining()){
            glob.mark();
            ch = glob.get();
            
            switch(ch){
            
            case '*':
                glob.reset(); //Unget the star
                break;
            
            case '\\':
                if(!glob.hasRemaining())
                    throw new GlobParsingException("Dangling escape character in glob (backslash)");
                ch = glob.get();
                //fall through to default:
                
            default:
                tmp.append(ch);
                ++len;
                continue;
            }
            break;
        }
        
        m_substring = tmp.limit(len).rewind().toString();
    }
    
    @Override
    public boolean push(CharBuffer buf) {
        int count = Math.min( buf.remaining(),  m_substring.length() - pos );
        boolean result = false;
        char bc;
        
        while( pos < m_substring.length()  && buf.hasRemaining() ){
            buf.mark();
            bc = buf.get();
            if(bc == m_substring.charAt(pos)){
                ++pos;
                result=true;
            }
            else{
                buf.reset();              
            }
        }
        return result;
    }

    @Override
    public boolean satisfied() {
        return pos >= m_substring.length();
    }

    @Override
    public void clear() {
        pos = 0;
    }

    @Override
    public CharBuffer get() {        
        return CharBuffer.wrap(m_substring, 0, pos);
    }
      
    public String substring(){
        return m_substring;
    }
    
}
