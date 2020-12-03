package com.levitator.oath_wallet_service.messages.common;

import java.lang.reflect.InvocationTargetException;
import java.util.TreeMap;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

/*
* Allows creation of message classes by name
*/
public class MessageFactory {
    static private MessageFactory s_instance;
    static private TreeMap<String, Class<? extends IMessage>> s_message_registry;
            
    private MessageFactory(){
        s_message_registry = new TreeMap<>();
    }
    
    static public MessageFactory instance(){
        if(s_instance == null)
            s_instance = new MessageFactory();
        return s_instance;
    }
    
    public <T extends IMessage> void add(String name, Class<? extends T> cls){
        if(s_message_registry.putIfAbsent(name, cls) != null)
            throw new RuntimeException("Duplicate message type of name: " + name);
    }
    
    public Class<? extends IMessage> get_class(String name){
        return s_message_registry.get(name);
    }
    
    public IMessage create(String name){
        var cls = get_class(name);
        
        try{
            var cons = cls.getConstructor();
            return cons.newInstance();
        }
        catch(NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new RuntimeException("Reflection error creating message object");
        }
    }
    
    //Assumes object start has been parsed
    public IMessage create(JsonParser parser){
        if(parser.next() != JsonParser.Event.KEY_NAME)
            throw new JsonParsingException("Expected key name", parser.getLocation());
        
        if(!parser.getString().equals("type"))
            throw new JsonParsingException("First element of typed message must be 'type'", parser.getLocation());
        
        var type = parser.getString();
        var obj = create(type);
        obj.parse(parser);
        return obj;
    }
    
}
