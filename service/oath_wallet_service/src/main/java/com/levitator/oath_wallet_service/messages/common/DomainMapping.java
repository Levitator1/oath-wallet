package com.levitator.oath_wallet_service.messages.common;

import com.levitator.oath_wallet_service.Parser;
import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.util.ComparablePair;
import com.levitator.oath_wallet_service.util.GlobParsingException;
import com.levitator.oath_wallet_service.util.SimpleGlobMatcher;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

public class DomainMapping implements Comparable, IMessage{
    
    private ComparablePair<String, String> data; //credential, and url
    private SimpleGlobMatcher m_matcher;
    
    public boolean valid(){
        return url() != null && url().length() > 0 && cred() !=null && cred().length() > 0;
    }

    DomainMapping(JsonParser parser){
        parse(parser);
    }
    
    @Override
    public void parse(JsonParser parser){
        data = new ComparablePair<>();
        Event evt;
        String property;
        
        //Assumes the start of object has already been picked up
        while((evt = parser.next()) != JsonParser.Event.END_OBJECT){
            
            if(evt == JsonParser.Event.KEY_NAME){
                property = parser.getString();
                if( property.equals("url") ){
                    url(Parser.demand_string(parser));
                }
                else if( property.equals("cred") ){
                    data.first = Parser.demand_string(parser);
                }
                else
                    throw new JsonParsingException("Unkown property in mapping entry: " + property, parser.getLocation());              
            }
            else
                throw new JsonParsingException("Expected a property name", parser.getLocation());
        }
        
        if(cred() == null)
            throw new JsonParsingException("Domain-mapping entry is missing required 'cred' field", parser.getLocation());
        
        if(url() == null)
            url("");
        
        try {
            m_matcher = new SimpleGlobMatcher(url());
        } catch (GlobParsingException ex) {
            m_matcher = null;
            Service.instance.log("URL specification for credential '" + cred() + "' was invalid: " + ex.getMessage());
        }
    }
    
    public SimpleGlobMatcher matcher(){
        return m_matcher;
    }
    
    @Override
    public JsonObjectBuilder toJson(){
        return Json.createObjectBuilder().add("url", url()).add(cred(), "cred");
    }
    
    //Credential is read-only because it's used as a lookup key
    public String url(){ return data.second; }
    public void url(String v){ data.second = v; }
    public String cred(){ return data.first; }

    @Override
    public int compareTo(Object t) {
        var rhs = (DomainMapping)t;
        return data.compareTo(rhs.data);
    }
}
