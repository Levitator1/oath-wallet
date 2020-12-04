package com.levitator.oath_wallet_service.util;

import java.io.IOException;
import java.io.OutputStream;

// Override write(byte[] b, int off, int len) to define IO
public abstract class OutputStreamBase extends OutputStream{

    byte[] one_byte = new byte[1];
    
    @Override
    public abstract void write(byte[] b, int off, int len) throws IOException;

    @Override
    public void write(byte[] b) throws IOException {
        write( b, 0, b.length );
    }

    @Override
    public void write(int i) throws IOException {
        one_byte[0] = (byte) i;
        write(one_byte, 0, 1);
    }
}
