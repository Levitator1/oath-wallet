package com.levitator.oath_wallet_service.messages.common;

import com.levitator.oath_wallet_service.Parser;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.TreeMap;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
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
    
    //Retrieves the type() or protocol-level name of the class
    //Is implemented by creating an instance and returning type()
    static public <T extends TypedMessage> String message_name(Class<T> cls){        
        try{
            var cons = cls.getConstructor();
            var obj = cons.newInstance();
            return obj.type();
        }
        catch(NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex){
            throw new RuntimeException("Reflection error");
        }
    }
    
    public <T extends TypedMessage> void add( List<Class<? extends T>> cls_array ){
        for(var cls : cls_array){
            add(cls);
        }
    }
    
    public <T extends TypedMessage> void add(Class<? extends T> cls){
        var name = message_name(cls);
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
        var type = Parser.demand_string_named(parser, "type");                
        var obj = create(type);
        obj.parse(parser);
        
        //Object can't collect its own end tag, otherwise it couldn't be extended through inheritance
        if( parser.next() != Event.END_OBJECT )
            throw new JsonParsingException("Expected end of object", parser.getLocation());
        
        return obj;
    }
    
}
