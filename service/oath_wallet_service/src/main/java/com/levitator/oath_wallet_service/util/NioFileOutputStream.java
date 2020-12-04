package com.levitator.oath_wallet_service.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;

/**
 *
 * Like FileOutputStream, but interruptible
 * 
 */
public class NioFileOutputStream extends OutputStreamBase{

    private FileChannel m_channel;
    static private final OpenOption[] append_opts = new OpenOption[]{ StandardOpenOption.WRITE, StandardOpenOption.APPEND };
    static private final OpenOption[] truncate_opts = new OpenOption[]{ StandardOpenOption.WRITE };
    
    public NioFileOutputStream(File file, boolean append) throws IOException{                        
        m_channel = FileChannel.open(file.toPath(), append ? append_opts : truncate_opts );
    }
    
    //Non-append/truncate by default
    public NioFileOutputStream(File file) throws IOException{
        this(file, false);
    }
    
    @Override
    public void close() throws IOException {
        m_channel.close();
    }

    @Override
    public void flush() throws IOException {
        try{
            m_channel.force(true);
        }
        catch(Exception ex){
            //TODO: At least maybe log this. This implementation is meant for use with fifos
            //and, I guess they don't flush. So, we try anyway, and then throw out the resulting exception.
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        var buf = ByteBuffer.wrap(b, off, len);
        m_channel.write(buf);
    }
}
