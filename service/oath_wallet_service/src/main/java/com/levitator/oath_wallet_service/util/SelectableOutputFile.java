package com.levitator.oath_wallet_service.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import jdk.nio.Channels;

/**
 *
 * Obtain a SelectableChannel for output from a file, for use with a NIO Selector,
 * and so that you can adjust the blocking mode.
 * Seems to work on Linux OpenJDK. Elsewhere, who knows?
 * 
 */
public class SelectableOutputFile implements AutoCloseable{    
    private final FileOutputStream fos;
    private final SelectableChannel chan;
       
    public SelectableOutputFile(File path) throws FileNotFoundException, IOException{        
        fos = new FileOutputStream(path);                                
        chan = Channels.readWriteSelectableChannel(fos.getFD(), new NIOCloser(fos));                        
    }
    
    public FileOutputStream input_stream(){
        return fos;
    }
    
    public SelectableChannel channel(){
        return chan;
    }
    
    @Override
    public void close() throws Exception {
        chan.close();
        fos.close();
    }
    
}