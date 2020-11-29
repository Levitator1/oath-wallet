package com.levitator.oath_wallet_service.util;

//Recognizes only * and \ as special characters

import com.levitator.oath_wallet_service.util.impl.StarMatcher;
import com.levitator.oath_wallet_service.util.impl.StringMatcher;
import com.levitator.oath_wallet_service.util.impl.SubstringMatcher;
import java.nio.CharBuffer;
import java.util.ArrayList;

// * means "any substring"
// \ is an escape
// \* means to match the literal star character
// \\ means to match the literal backslash
public class SimpleGlobMatcher {
    private ArrayList<StringMatcher> matchers = new ArrayList<>();
    
    public SimpleGlobMatcher(String glob) throws GlobParsingException{        
        CharBuffer buf = CharBuffer.wrap(glob);
        
        while(buf.hasRemaining()){
            if( buf.charAt(0) == '*' ){
                buf.get();
                
                //Collapse redundant stars
                if(!(matchers.get( matchers.size() - 1 ) instanceof StarMatcher))
                    matchers.add( new StarMatcher() );
            }
            else
                matchers.add( new SubstringMatcher(buf) );
        }        
    }
    
    private boolean is_previous_star(int i){
        return i > 0 && matchers.get(i-1) instanceof StarMatcher;
    }
   
    public boolean match(String str){
    
        //Empty pattern never matches
        if(matchers.size() < 1)
            return false;
        
        CharBuffer buf = CharBuffer.wrap(str);
        
        int pos;
        StringMatcher matcher;
        int i;
        
        //Iterate over all but the last matcher
        for(i = 0; i < matchers.size() - 1; ++i){                                    
            matcher = matchers.get(i);
            
            //Star matcher is always satisfied, so skip him for now
            if(matcher instanceof StarMatcher){                     
                continue;
            }
            else{
                //Match a specific substring
                var subm = (SubstringMatcher)matcher;                
                pos = buf.toString().indexOf(subm.substring());
                
                //Failed matching. No match.
                if(pos < 0)
                    return false;
                
                //There is junk prior to the match, but there is no previous star matcher to stuff the junk into
                //So, fail.
                if(pos > 0 &&  !is_previous_star(i) )
                    return false;
                
                //We found a match and if there is any preceding junk, it matches a previous star
                buf.position( buf.position() + pos + subm.substring().length() );                                                
            }
        }
        
        //Process the last matcher as a special case
        matcher = matchers.get(i);
        
        //If the last matcher is star, then it eats anything at the end of the buffer
        //and that's a match
        if(matcher instanceof StarMatcher)
            return true;
        
        //Otherwiwse, the last matcher is for a specific substring
        var subm = (SubstringMatcher)matcher;
        var bufstr = buf.toString();
                    
        //Does the buffer end with the final match?
        if(bufstr.endsWith(subm.substring())){
            //Is it an exact match at the end of the buffer?
            if(buf.remaining() == subm.substring().length())
                return true;
            else{
                //If the end matches possessing preceding junk, then that's ok as long
                //as the previous matcher is a star, to eat the junk
                return is_previous_star(i);
            }
        }
        else
            return false;
    }    
}
