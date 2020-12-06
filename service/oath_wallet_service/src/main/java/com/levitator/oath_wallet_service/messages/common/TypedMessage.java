
package com.levitator.oath_wallet_service.messages.common;

import com.levitator.oath_wallet_service.Parser;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;

//A message which is serialized as a Json object which always begins with an element named "type"
//containing a string which identifies the type of message that it is

//parse() assumes that the object start and type field have already been parsed out

public interface TypedMessage extends IMessage{    
        
    //A unique type name which must be registered with MessageFactory in order 
    //to create these by name, from incoming JSON streams
    public String type();    
    
    //We don't do this because the factory has to do it in order to know
    //Which message class to create. The type is hard-coded into each class
    /*
    @Override
    public default void parse(JsonParser parser){
       type( Parser.demand_string_named(parser, "type") );
    }
    */
    
    @Override
    public default JsonObjectBuilder toJson() {
        var builder = Json.createObjectBuilder();
        builder.add("type", type());
        return builder;
    }
}
