package com.levitator.oath_wallet_service;

import java.io.EOFException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;

/**
 *
 * Serve as a dumb I/O relay between the browser and some other instance of this
 * process which has already taken the primary service role
 *
 */
public class PipeMode {

    Thread m_main_thread;
    LinkedTransferQueue<byte[]> to_service_queue, to_client_queue;
    Semaphore sem_to_service, sem_to_client;    
    
    public PipeMode(){
        m_main_thread = Thread.currentThread();
        to_service_queue = new LinkedTransferQueue<>();
        to_client_queue = new LinkedTransferQueue<>();
        sem_to_service = new Semaphore(0);
        sem_to_client = new Semaphore(0);        
    }
            
    
    //Rather than figure out how to do asynchronous/select stuff on stdio, we 
    //dedicate a thread to each direction
    private void stdout_thread_proc(){
        
        try{
            while(true){
                sem_to_client.acquire();
                var buf = to_client_queue.take();
                System.out.write(buf);
            }
        }
        catch(Exception ex){
            //There's basically only one thing that can go wrong in this mode, and that's IO error
            //So, if we get an error, we clean up and exit
            m_main_thread.interrupt();
        }        
    }
    
    private void stdin_thread_proc(){
        var buf = new byte[1024];
        int ct;
        try{
            while(true){
                ct = System.in.read(buf);
                if(ct < 1)
                    throw new EOFException();
                
                var buf2 = Arrays.copyOf(buf, ct);
                to_service_queue.add(buf2);
                sem_to_service.release();
            }
        }
        catch(Exception ex){
            m_main_thread.interrupt();
        }
    }
    
    public void run(){
        
        //Launch the two stdio threads
        var stdin_thread = new Thread( ()->{ stdin_thread_proc(); }, "Pipe Mode stdin");
        var stdout_thread = new Thread( ()->{ stdout_thread_proc(); }, "Pipe Mode stdout");        
        stdin_thread.run();
        stdout_thread.run();
        
        
        
    } 
    
}
