package com.levitator.oath_wallet_service.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// In order to define all IO, just overload: public abstract int read(byte[] b, int off, int len) throws IOException;
//
public abstract class InputStreamBase extends InputStream{

    private byte[] one_byte = new byte[1];
    
    @Override
    public int read() throws IOException {
        //-1 for EOF
        
        //Should always return -1 or 1, and not 0
        var ret = read(one_byte, 0, 1);
        if(ret == -1)
            return -1;
        else if( ret == 1)
            return one_byte[0];
        else
            throw new IOException("Single-byte read returned something other than 1 or EOF"); //Should never happen
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return super.transferTo(out); //To change body of generated methods, choose Tools | Templates.
    }

    //Returns 0 on EOF
    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        
        int ret;
        int i = off;
        while(len > 0){
            ret = read(b, i, len);
            if(ret == -1)
                break;
            else{
                i+=ret;
                len-=ret;
            }            
        }
        return i;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        
        //Break up the operation in case someone specifies crazy len
        var os = new ByteArrayOutputStream();
        var tmp = new byte[4096];
        
        while( os.size() < len ){
            var ret = read(tmp);
            if(ret < 1)
                break;
            os.write(tmp, 0, ret);
            len -= ret;
        }
                
        return os.toByteArray();
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return readNBytes(Integer.MAX_VALUE);
    }

    @Override
    public abstract int read(byte[] b, int off, int len) throws IOException;

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }        
}
