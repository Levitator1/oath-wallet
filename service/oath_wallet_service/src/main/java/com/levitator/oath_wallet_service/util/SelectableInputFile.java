package com.levitator.oath_wallet_service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import jdk.nio.Channels;

/**
 *
 * Obtain a SelectableChannel for input from a file, for use with a NIO Selector,
 * and so that you can adjust the blocking mode.
 * Seems to work on Linux OpenJDK. Elsewhere, who knows?
 * 
 */
public class SelectableInputFile implements AutoCloseable{
    private final FileInputStream fis;
    private final SelectableChannel chan;
       
    public SelectableInputFile(File path) throws FileNotFoundException, IOException{        
        fis = new FileInputStream(path);                                
        chan = Channels.readWriteSelectableChannel(fis.getFD(), new NIOCloser(fis));                
    }
    
    public FileInputStream input_stream(){
        return fis;
    }
    
    public SelectableChannel channel(){
        return chan;
    }

    @Override
    public void close() throws IOException {
        chan.close();
        fis.close();
    }
    
}
