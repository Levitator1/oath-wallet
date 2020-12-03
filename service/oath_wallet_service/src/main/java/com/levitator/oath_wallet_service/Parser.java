package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.util.Pair;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

public class Parser {
     
        static public Pair<FileInputStream, JsonParser> parserFromFile(File file) throws FileNotFoundException, IOException{
                 
        FileInputStream is;
        try{
            if(file.length() < 1){                
                throw new EOFException("File is empty");
            }
            else
                is = new FileInputStream(file);                                     
        }
        catch(FileNotFoundException | EOFException ex){
            Service.instance.log("Config file not found: '" + file + "'. Creating empty.");            
            try{
                var dir = file.getParentFile();
                dir.mkdirs();                
                file.createNewFile();
                is = new FileInputStream(file);                                
            }
            catch(IOException ex2){
                Service.instance.fatal("Could not create configuration file: " + file + "\n Reason: " + ex2.getLocalizedMessage());
                throw ex2;
            }
            
            //We're going to get a JsonException below because the parser can't deal with
            //empty files. So, let's make it specific so that we can work around it.
            throw new EOFException("Empty file");
        }
        
        return new Pair<>(is, Json.createParser(is));
    }
    
    static public JsonParser parserFromString(String data){
        return Json.createParser( new StringReader(data) );
    }
    
    static public String demand_string(JsonParser parser){
        var evt = parser.next();
        if(evt != Event.VALUE_STRING)
            throw new JsonParsingException("Expected string while parsing", parser.getLocation());
        return parser.getString();
    }
    
    //Demand a key of a specific name
    static public void demand_key_named(JsonParser parser, String name){        
        if(parser.next() != Event.KEY_NAME)
            throw new JsonParsingException("Expected a key name", parser.getLocation());
        
        var key = parser.getString();
        if(!key.equals(name))
            throw new JsonParsingException("Expected a key named '" + name + "' but got '" + key + "' instead", parser.getLocation());
    }
    
    static public void demand_event(JsonParser parser, Event type){
        var nx = parser.next();
        if( !nx.equals(type) )
            throw new JsonParsingException("Expected JsonParser event type #" + type.toString()
                    + ". Got : " + nx.toString(), parser.getLocation());
    }
    
    //Demand a string field having the named key
    static public String demand_string_named(JsonParser parser, String name){
        demand_key_named(parser, name);
        demand_event(parser, Event.VALUE_STRING);
        return parser.getString();   
    }

    public static long demand_long_named(JsonParser parser, String name) {
        demand_key_named(parser, name);
        demand_event(parser, Event.VALUE_NUMBER);
        return parser.getLong();
    }
    
    public static void demand_end_object(JsonParser parser){
        if(parser.next() != Event.END_OBJECT)
            throw new JsonParsingException("Expected to find end of object here", parser.getLocation());
    }
    
}
