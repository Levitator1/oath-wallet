package com.levitator.oath_wallet_service.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/*
*
* InputStream implemented with NIO so that it's guranteed interruptible
*
*/
public class NioFileInputStream extends InputStreamBase implements AutoCloseable{

   //SelectableInputFile m_file;
   FileChannel m_in_chan;
   private long pos;
  
    public NioFileInputStream(File file) throws IOException{
        m_in_chan = FileChannel.open(file.toPath(), StandardOpenOption.READ );
        pos = 0;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException{
        var buf = ByteBuffer.wrap(b, off, len);
        int result = m_in_chan.read(buf);
        if(result > 0)
            pos += buf.remaining();
        
        //Hopefully no more 0 returns since the file is in blocking mode
        return result;
    }

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
        m_in_chan.close();
    }

    @Override
    public int available() throws IOException {
        return (int)Math.min(m_in_chan.size() - pos, Integer.MAX_VALUE);        
    }

    @Override
    public long skip(long n) throws IOException {
        
        //I doubt that position() is smart enough to discard bytes when seeking forward
        //in order to accomodate non-seekable streams, so we will do the safe thing and read
        //and toss the data
        var remain = n;
        var buf = ByteBuffer.allocate(4096);
        int result;
        while(remain > 0){
            buf.position( (int)Math.min(buf.capacity(), remain) );
            result = m_in_chan.read(buf);
            if(result < 0)
                break;
            else
                remain -= result;
        }
        var ct = n - remain;
        pos += ct;        
        return ct;
    }    
}
