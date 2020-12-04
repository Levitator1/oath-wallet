package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.messages.common.DomainMapping;
import com.levitator.oath_wallet_service.messages.common.DomainMappingConfig;
import com.levitator.oath_wallet_service.messages.common.DomainMappingConfig.Mappings;
import com.levitator.oath_wallet_service.util.Pair;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.stream.JsonParser;

public class DomainMapper implements AutoCloseable{
    
    //We hold this exclusive lock for the lifetime of DomainMapper so that other program
    //instances know that an instance already has ownership of the daemon and front-end roles
    private FileOutputStream map_file_ostream; //Has to be an output stream to provide an exclusive lock
    private FileChannel map_file_channel;
    private FileLock map_file_lock;
    
    private DomainMappingConfig config;
    
    public DomainMapper() throws FileNotFoundException, IOException, ConfigLockedException{
        
        //Acquire an exclusive file lock. We don't actually write with this stream. It's just to hold the lock.
        map_file_ostream = new FileOutputStream(Config.instance.domain_config, true);
        map_file_channel = map_file_ostream.getChannel();
        map_file_lock = map_file_channel.tryLock();
        if(map_file_lock == null)
            throw new ConfigLockedException();
        
        Pair<FileInputStream, JsonParser> data;
        try{
            data = Parser.parserFromFile(Config.instance.domain_config);
        }
        catch(EOFException ex){
            //Populate the mapping file so that we can load it
            config = new DomainMappingConfig();
            save();
            data = Parser.parserFromFile(Config.instance.domain_config);
        }
                        
        try{
            config = new DomainMappingConfig(data.second);
        }
        catch(Exception ex){                                    
            Service.instance.log("Parse error", null, ex);
            Service.instance.log("It looks like you used to have a working domain mapping file, but it got corrupt. " +
                "You will need to delete it and restart the program. Your mappings will have to be re-entered. " +
                "Alternately, if there is an '.old' version of the file in the configuration directory, then you can copy it " +
                "(without the '.old' suffix) to revert to a previous version of the configuration, then relaunch the program.\n");
            throw ex;
        }        
    }
    
    public void save() throws FileNotFoundException, IOException{
        
        var tmp = new File(Config.instance.domain_config.toString() + ".tmp");        
        var old = new File( Config.instance.domain_config.toString() + ".old" );                
        var tmp_stream = new FileOutputStream(tmp);
        var tmp_stream_lock = tmp_stream.getChannel().lock();
                
        JsonWriter tmp_writer = null;
        tmp_writer = Json.createWriter(tmp_stream);
        
        try{            
            tmp_writer.writeObject( config.toJson().build() );            
            Config.instance.domain_config.renameTo( old );
            tmp.renameTo( Config.instance.domain_config );
            map_file_lock.close();

            //TODO: Make sure that the lock is preserved across the rename operation
            //Since it's probably implemented using file descriptors, then hopefully it works
            map_file_lock = tmp_stream_lock; 
            Service.instance.log( "Updated configuration: " + Config.instance.domain_config);
        }
        finally{
            if(tmp_writer != null)
                tmp_writer.close();
             
            tmp_stream.close();
        }
    }
    
    public Mappings mappings(){
        return config.mappings;
    }

    @Override
    public void close() throws IOException {
        map_file_lock.close();      //probably superfluous
        map_file_channel.close();
        map_file_ostream.close();
    }
    
    public ArrayList<DomainMapping> map(String url){
        var result = new ArrayList<DomainMapping>();
        
        for(var entry : config.mappings){
            if( entry.matcher() != null && entry.matcher().match(url) )
                result.add(entry);
        }
        return result;
    }
    
}
