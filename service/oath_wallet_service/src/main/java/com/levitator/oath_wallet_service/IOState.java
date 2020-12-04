package com.levitator.oath_wallet_service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import javax.json.Json;

public class IOState{
    
    private Service m_service;
    private AsynchronousFileChannel m_channel;
    private ByteArrayOutputStream m_in_data;
    private ByteBuffer m_in_buf;
    private long readpos = 0;
        
    public IOState(Service serv){
        m_service = serv;               
    }

    void new_session() throws IOException{
        
        m_channel = AsynchronousFileChannel.open(Config.instance.fifo_path, StandardOpenOption.APPEND, StandardOpenOption.READ);
        m_in_buf = ByteBuffer.wrap( new byte[1024] );
        readpos=0;
    }
    
    private void fetch_message() throws InterruptedException, ExecutionException {
        
        //Hopefully this will return partial data instead of demanding a full buffer
        m_in_buf.limit( m_in_buf.capacity() );
        m_in_buf.rewind();
        var read_future = m_channel.read(m_in_buf, readpos);
        read_future.get();
        var count = m_in_buf.position();
        readpos += count;
        m_in_buf.flip();
        m_in_data.write(m_in_buf.array(), m_in_buf.arrayOffset(), count);
        
        //This is kind of terrible but I don't know how else to make it interruptible
        /*
        try{
            var is = new ByteArrayInputStream( m_in_data.toByteArray() );
            var parser = Json.createParser(is);
            
        }
        */
    }
    
    private void input_loop(){
        while(true){
                        
            //fetch_message();
            
        }
    }
    
    //@Override
    public void run() {
        try{
           new_session();
           input_loop();
        }
        catch(Exception ex){
            m_service.log("Unexpected exception in service input thread. Exiting.", null, ex);
            m_service.exit(-1);
        }         
    }
    
}
