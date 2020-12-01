package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.util.Pair;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

public class Parser {
    
        static public Pair<InputStream, JsonParser> parserFromFile(File file) throws FileNotFoundException, IOException{
                 
        FileInputStream is;
        try{
            is = new FileInputStream(file);
            
            //shared lock for reading. Implicitly gets cleaned up when the file is closed.
            is.getChannel().lock(0, Long.MAX_VALUE, true); 
        }
        catch(FileNotFoundException ex){
            Service.log("Config file not found: " + file);
            Service.log("Creating empty");
            try{
                var dir = file.getParentFile();
                dir.mkdirs();                
                file.createNewFile();
                is = new FileInputStream(file);
                Service.log("OK");
            }
            catch(IOException ex2){
                Service.fatal("Could not create configuration file: " + file + "\n Reason: " + ex2.getLocalizedMessage());
                throw ex2;
            }
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
    
}
