package com.levitator.oath_wallet_service.messages.common;
import com.levitator.oath_wallet_service.messages.common.DomainMapping;

import java.util.TreeSet;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

public class DomainMappingConfig implements IMessage {
    
    public DomainMappingConfig(JsonParser parser){
        parse(parser);
    }
    
    public DomainMappingConfig(){        
    }
    
    @Override
    public void parse(JsonParser parser){
        //Nothing to do with an empty configuration
        if(!parser.hasNext())
            return;
        
        var evt = parser.next();
        if(evt != JsonParser.Event.START_OBJECT){            
            throw new JsonParsingException("Expected the first element in the file to be an object. Parsing event: " + evt, parser.getLocation());            
        }
        
        evt = parser.next();
        if(evt != Event.KEY_NAME || !parser.getString().equals("mappings"))
            throw new JsonParsingException("Expected a property named 'mappings'", parser.getLocation());
        
        evt= parser.next();
        if(evt != Event.START_ARRAY)
            throw new JsonParsingException("Expected 'mappings' property to contain an array", parser.getLocation());

        DomainMapping mapping;
        while((evt = parser.next()) == Event.START_OBJECT){
            mapping = new DomainMapping(parser);
            mappings.add(mapping);
        }
       
        if(evt != Event.END_ARRAY)
            throw new JsonParsingException("Expected another mapping object or the end of the array", parser.getLocation());
        
        evt = parser.next();
        if(evt != Event.END_OBJECT || parser.hasNext())
            throw new JsonParsingException("Encountered junk following the mapping array", parser.getLocation());        
    }
    
    @Override
    public JsonObjectBuilder toJson(){
        var array = Json.createArrayBuilder();
        for(var m : mappings ){
            array.add( m.toJson() );
        }
        
        return Json.createObjectBuilder().add("mappings", array);
    }
    
    public class Mappings extends TreeSet<DomainMapping>{}
    public final Mappings mappings = new Mappings();
    
}
