package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.messages.out.ErrorMessage;
import com.levitator.oath_wallet_service.util.ThrowingConsumer;
import com.levitator.oath_wallet_service.util.Util;
import java.io.Closeable;
import java.io.EOFException;
import java.nio.channels.SelectableChannel;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import jdk.nio.Channels;

/**
 *
 * Serve as a dumb I/O relay between the browser and some other instance of this
 * process which has already taken the primary service role
 *
 */

//Is not required to launch as a thread, but can
public class PipeMode extends Thread implements AutoCloseable{

    //Does nothing if not running as a thread
    @Override
    public void close() throws IOException, InterruptedException {
        
        ThrowingConsumer<Exception, RuntimeException> warn = (ex)->Service.instance.log("WARNING: IO failed to close in relay mode", null, ex);
        
        //
        // IO cleanup 
        //
        Util.with_handler( ()->System.in.close(), warn );
        Util.with_handler( ()->System.out.close(), warn );
        Service.instance.log("STDIO should be closed here");
        Util.with_handler( ()->{ if(selector!=null)selector.close(); }, warn );
        Util.with_handler( ()->{ if(server_to_client != null) server_to_client.close(); }, warn);
        Util.with_handler( ()->{ if(client_to_server != null) client_to_server.close(); }, warn);
                
        //
        // Thread cleanup below
        //
        if(!isAlive())
            return;
        
        //Open the other ends of the pipes to unstall the thread in case it wasn't connected
        //Async close() is another option, which hopefully NIO would notice and unblock from
        if(Thread.currentThread() != this){
            interrupt();
            try(
                var tmp = new FileOutputStream(Config.instance.fifo_out_path.toFile(), true);
                var tmp2 = new FileInputStream(Config.instance.fifo_in_path.toFile());
            )
            {
                join(3000);
                if(!isAlive()){
                    Service.instance.log("Gave up waiting for stalled relay thread");
                }
            }
        }        
    }

    static class InputState implements AutoCloseable{
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

        @Override
        public void close() throws IOException{
            out.close();
            key.cancel();
            channel.close();
            stream.close();
        }
    }
    
    private FileOutputStream iolog;
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
    
    private void check_interrupt() throws InterruptedException{
        if(Thread.interrupted())
            throw new InterruptedException();
    }
                   
    //Irks me slightly that writes are synchronous and blocking, but there is probably
    //platform buffering and the messages are small
    private void do_io(InputState state) throws EOFException, IOException{       
        buffer.limit( buffer.capacity() );
        buffer.rewind();
        int count = state.stream.read(buffer);
        if(count < 0)
            throw new EOFException();
        buffer.flip();
        state.out.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        state.out.flush();
        
        String msg;
        if(state == server_to_client){
            iolog.write( 'O');            
        }        
        else
            iolog.write( 'I' );
        
        iolog.write(':');
        iolog.write(' ');
        
        var data = Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.remaining());        
        iolog.write(data);
        iolog.write('\n');
        iolog.flush();
    }
    
    private void io_loop() throws EOFException, IOException, LockException, InterruptedException{
        try(var lock = new IPCLock(Config.instance.client_lock_path);){
            
            boolean DEBUG = true;
            
            while(!DEBUG){
                Thread.sleep(1000);
            }
            
            while(true){
                selector.select();
                var keys = selector.selectedKeys();

                if(keys.contains(client_to_server.key)){                    
                    do_io(client_to_server);                    
                }

                if(keys.contains(server_to_client.key))
                    do_io(server_to_client);
            }
        }
        catch(EOFException ex){
            //I'm suprised that this has to be explicitly caught and rethrown for the calling function
            //to see it as such. I guess it's upcast to IOException otherwise.
            throw ex;
        }
    }

    //run() interface with extra exceptions for same-threaded usage
    public void same_thread_run() throws IOException, LockException, InterruptedException{
        
        try{
            //Initialization which might otherwise go in the constructor, but since it could block,
            //we put it on this code path instead
            iolog = new FileOutputStream( "/home/j/iolog", true );
            m_main_thread = Thread.currentThread();
            var fifo_out = new PrintStream(new FileOutputStream(Config.instance.fifo_in_path.toFile(), true));
            check_interrupt();
            var fifo_is = new FileInputStream(Config.instance.fifo_out_path.toFile());
            check_interrupt();
            var fifo_in = fifo_is.getChannel();            
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
        
            io_loop();    
        }
        catch( EOFException ex ){
            //Consider EOF a normal exit for our purposes
            Service.instance.log("Connection relay clean exit for EOF");
        }
        catch(LockException ex){
            //One client at a time
            var msg = new ErrorMessage("Connection busy. Sorry, try again.", 0);
            Service.send_message(msg, System.out);
        }
        catch(IOException ex){
            Service.instance.log("Connection relay closing for IO error");
            throw ex;
        }
        catch(InterruptedException ex){
            Service.instance.log("Connection relay exits for interrupt");
            throw ex;
        }
        finally{
            close();
        }
    }
    
    @Override
    public void run() {
        try{
            same_thread_run();
        }
        catch(Exception ex){            
            //All exceptions have already been reported and, as a thread, there is nobody
            //left to catch, so just exit
        }
    }
}
