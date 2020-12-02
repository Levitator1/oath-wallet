package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.util.Pair;
import java.io.EOFException;
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
    
}
