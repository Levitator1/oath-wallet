package com.levitator.oath_wallet_service.util;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;

/*
*
* InputStream implemented with NIO so that it's guranteed interruptible
*
*/
public class NioFileInputStream extends InputStreamBase{

    private FileChannel m_channel;
    long m_pos;
    
    public NioFileInputStream(File file) throws IOException{
        m_channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        m_pos = 0;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException{
        var buf = ByteBuffer.wrap(b, off, len);
        return m_channel.read(buf);
    }
    
    /*
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            var buf = ByteBuffer.wrap(b, off, len);
            var promise = m_channel.read(buf, m_pos);
            promise.get();            
            return buf.position();
        } catch (InterruptedException ex) {
            throw new InterruptedIOException("NioFileInputStream interrupted");
        } catch (ExecutionException ex) {            
            throw new IOException("Execution exception in NioFileInputStream");
        }
    }
    */

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark() not supported");
    }

    @Override
    public synchronized void mark(int readlimit) {        
    }

    @Override
    public void close() throws IOException {
        m_channel.close();
    }

    @Override
    public int available() throws IOException {
        return (int)Math.min(m_channel.size() - m_pos, Integer.MAX_VALUE);        
    }

    @Override
    public long skip(long n) throws IOException {
        m_pos += n;
        return n;
    }    
}
