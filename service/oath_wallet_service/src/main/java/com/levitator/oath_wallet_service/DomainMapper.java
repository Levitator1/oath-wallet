package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.messages.common.DomainMappingConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.json.Json;
import javax.json.JsonWriter;

public class DomainMapper {
              
    public DomainMapper() throws FileNotFoundException, IOException{   
        var data = Parser.parserFromFile(Config.instance.domain_config);
        
        try(var stream=data.first; var parser=data.second){
            config = new DomainMappingConfig(parser);
        }
        catch(Exception ex){
            Service.fatal("It looks like you used to have a working domain mapping file, but it got corrupt. " +
                "You will need to delete it and restart the program. Your mappings will have to be re-entered. " +
                "Also, if there is an '.old' version of the file in the configuration directory, then you can rename it " +
                "to revert to a previous version of the configuration.", ex);
            throw ex;
        }        
    }
    
    public void save() throws FileNotFoundException, IOException{
        
        var tmp = new File(Config.instance.domain_config.toString() + ".tmp");        
        var old = new File( Config.instance.domain_config.toString() + ".old" );
        var stream = new FileOutputStream(tmp);
        
        JsonWriter writer = null;
        try(var writer_clean=writer; var stream_clean=stream ){
            writer = Json.createWriter(stream);
            writer.writeObject( config.toJson().build() );
        }
       
        Config.instance.domain_config.renameTo( old );
        tmp.renameTo( Config.instance.domain_config );
        Service.log( "Updated configuration: " + Config.instance.domain_config);
    }
    
    private DomainMappingConfig config;    
}
