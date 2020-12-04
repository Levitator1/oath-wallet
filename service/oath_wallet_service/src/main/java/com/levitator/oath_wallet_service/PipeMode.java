package com.levitator.oath_wallet_service;

import java.io.Closeable;
import java.io.EOFException;
import java.nio.channels.SelectableChannel;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import jdk.nio.Channels;

/**
 *
 * Serve as a dumb I/O relay between the browser and some other instance of this
 * process which has already taken the primary service role
 *
 */
public class PipeMode {

    static class InputState{
        public FileChannel stream;
        public SelectableChannel channel;
        public SelectionKey key;
        public PrintStream out;
        
        public InputState(FileChannel in, SelectableChannel chan, SelectionKey k, PrintStream out_stream){
            stream = in;
            channel = chan;
            key = k;
            out = out_stream;
        }
    }
    
    private Thread m_main_thread;
    private InputState client_to_server, server_to_client;
    private Selector selector;  
    private ByteBuffer buffer;

    class IOCloser implements Channels.SelectableChannelCloser{

        private Closeable m_stream;
        
        IOCloser(Closeable stream){
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
    
    public PipeMode() throws FileNotFoundException, IOException{
        m_main_thread = Thread.currentThread();
        var fifo_is = new FileInputStream(Config.instance.fifo_path.toFile());
        var fifo_in = fifo_is.getChannel();
        var fifo_out = new PrintStream(new FileOutputStream(Config.instance.fifo_path.toFile()));        
        
        //stdout_channel = Channels.readWriteSelectableChannel​(FileDescriptor.out, new IOCloser(null));                
        //fifo_out_channel = Channels.readWriteSelectableChannel(fifo_out.getFD(), new IOCloser(fifo_out_channel));
        var stdin_file_channel = new FileInputStream(FileDescriptor.in).getChannel();
        SelectableChannel fifo_in_channel = Channels.readWriteSelectableChannel(fifo_is.getFD(), new IOCloser(fifo_is));
        fifo_in_channel.configureBlocking(false);
        SelectableChannel stdin_channel = Channels.readWriteSelectableChannel​(FileDescriptor.in, new IOCloser(null));
        stdin_channel.configureBlocking(false);
        selector = Selector.open();
        
        var stdin_key = stdin_channel.register(selector, SelectionKey.OP_READ);
        var fifo_in_key = fifo_in_channel.register(selector, SelectionKey.OP_READ);
        buffer = ByteBuffer.wrap(new byte[512]);
        
        client_to_server = new InputState(stdin_file_channel, stdin_channel, stdin_key, fifo_out);
        server_to_client = new InputState(fifo_in, fifo_in_channel, fifo_in_key, System.out);
    }                    
    
    private void do_io(InputState state) throws IOException{
        
        //state.channel.keyFor(selector).cancel();
        //state.channel.configureBlocking(true);
        //selector.select(0); //Commit the key cancellation
        try{
            buffer.limit( buffer.capacity() );
            buffer.rewind();
            int count = state.stream.read(buffer);
            if(count < 0)
                throw new EOFException();
            buffer.flip();
            state.out.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
            state.out.flush();            
        }
        finally{
            //state.channel.configureBlocking(false);
            //state.key = state.channel.register(selector, SelectionKey.OP_READ);
        }
    }
    
    private void io_loop() throws IOException{
        while(true){
            selector.select();
            var keys = selector.selectedKeys();
            
            if(keys.contains(client_to_server.key))
                do_io(client_to_server);
            
            if(keys.contains(server_to_client.key))
                do_io(server_to_client);
        }
    }
    
    public void run() throws IOException{
        io_loop();
    } 
    
}
