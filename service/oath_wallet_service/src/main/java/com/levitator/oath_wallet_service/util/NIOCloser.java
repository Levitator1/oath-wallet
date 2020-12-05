package com.levitator.oath_wallet_service.util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import jdk.nio.Channels;

/**
 *
 * For use with NIO SelectableChannel
 * This just calls close() on a Closeable when the channel decides it's ready to close
 * I don't know what implReleaseChannel() is supposed to accomplish, so it does nothing
 * 
 */
class NIOCloser implements Channels.SelectableChannelCloser{

     private Closeable m_stream;

     NIOCloser(Closeable stream){
         m_stream = stream;
     }

     @Override
     public void implCloseChannel(SelectableChannel sc) throws IOException {
         if(m_stream != null)
             m_stream.close();
     }

     @Override
     public void implReleaseChannel(SelectableChannel sc) throws IOException {       
     }        
 }
