
package com.levitator.oath_wallet_service.messages.common;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

//A message which is serialized as a Json object which always begins with an element named "type"
//containing a string which identifies the type of message that it is

//parse() assumes that the object start and type field have already been parsed out

public interface TypedMessage extends IMessage{    
    
    //A unique type name which must be registered with MessageFactory in order 
    //to create them by name, from incoming JSON streams
    public String type();    
    
    public default JsonObjectBuilder toJson() {
        var builder = Json.createObjectBuilder();
        builder.add("type", type());
        return builder;
    }
}
