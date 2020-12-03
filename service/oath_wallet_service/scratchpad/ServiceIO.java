package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.messages.common.IMessage;
import com.levitator.oath_wallet_service.messages.common.MessageFactory;
import com.levitator.oath_wallet_service.messages.common.TypedMessage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.json.Json;
import javax.json.JsonWriter;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

/*
*
* The complexity of this is getting kind of stupid. If we assume that communication
* will always take the form of pairs of <query> and <reply>, then this can be done synchronously
* and procedurally in one thread instead of three
*
*/

//Handle connection state to a client process over the named pipe
/*
public class ServiceIO {  
    //Stuff that gets initialized on construction
    private final Semaphore m_in_queue_semaphore, m_out_queue_semaphore;
    private ReentrantLock thread_state_lock;
    private Condition state_condition;
    private LinkedTransferQueue<IMessage> m_in_queue, m_out_queue;
    private Thread m_input_thread, m_output_thread;
       
    //Stuff that belongs to the IO threads
    private JsonParser m_input_parser;
    private JsonWriter m_output_writer;
    private FileInputStream m_in_stream = null;
    private FileOutputStream m_out_stream = null;
    enum ThreadState{
        RUN,        
        PARKED,
        EXIT
    }
    ThreadState m_requested_thread_state=ThreadState.RUN, m_in_state = ThreadState.RUN, m_out_state = ThreadState.RUN;
    
    //Connection-oriented stuff
    Runnable m_connect_handler = null;      //runs on the input thread
    Runnable m_disconnect_handler = null;   //could run on either the input or output thread
    Consumer<TypedMessage> m_message_in_handler = null; //runs on the input thread
    
    
    private void set_target_state( ThreadState target ){
        thread_state_lock.lock();
        try{
            m_requested_thread_state = target;
            state_condition.notifyAll();            
        }
        finally{
            thread_state_lock.unlock();
        }
    }
    
    private void await_thread_state() throws InterruptedException{
        thread_state_lock.lock();
        try{            
            while(m_in_state != m_requested_thread_state || m_out_state != m_requested_thread_state)
                state_condition.await();
        }
        finally{
            thread_state_lock.unlock();
        }        
    }
    
    //Kills the current connection state
    public void reset() throws InterruptedException{
        //First, we request both IO threads to park so that they will stop at the top of their loops
        set_target_state( ThreadState.PARKED );
        
        //Now we interrupt both threads in case they are stalled mid-IO. They will close their respective stream.
        m_input_thread.interrupt();
        m_output_thread.interrupt();
        
        //We confirm that the threads are parked
        await_thread_state();
        
        //Now we purge the message queues
        m_in_queue.clear();
        m_out_queue.clear();
        m_in_queue_semaphore.drainPermits();
        m_out_queue_semaphore.drainPermits();
        
        //Notify the application of a disconnect
        if(m_disconnect_handler != null)
            m_disconnect_handler.run();
        
        //Now we restart the threads
        set_target_state(ThreadState.RUN);        
    }
    
    
    public ServiceIO(){
        m_in_queue_semaphore = new Semaphore(0);
        m_out_queue_semaphore = new Semaphore(0);
        thread_state_lock = new ReentrantLock();        
        state_condition = thread_state_lock.newCondition();        
        m_in_queue = new LinkedTransferQueue<>();
        m_out_queue = new LinkedTransferQueue<>();
    }            
       
    private ThreadState pipe_in_message() throws FileNotFoundException, InterruptedException{
        
        ThreadState result;
        try{
            thread_state_lock.lock();
            result = m_requested_thread_state;
            switch(result){
                case PARKED:
                    m_in_state = ThreadState.PARKED;
                    state_condition.notifyAll();
                    while(m_requested_thread_state == ThreadState.PARKED)
                        state_condition.await();                                        
                    break;
                    
                case EXIT:
                    m_in_state = result;
                    return result;
                
                case RUN:
                default:
            }
            m_in_state = result;
        }
        finally{            
            thread_state_lock.unlock();
        }
        
        
        if(m_in_stream == null){
            m_in_stream = new FileInputStream(Config.instance.fifo_path.toFile());
            m_input_parser = Json.createParser(m_in_stream);
            m_connect_handler.run();
        }
        if(m_input_parser.next() != JsonParser.Event.START_OBJECT)
            throw new JsonParsingException("Expected an object", m_input_parser.getLocation());
        else{
            var msg = MessageFactory.instance().create(m_input_parser);
            m_in_queue.add(msg);
            m_in_queue_semaphore.release();
        }
        return result;
    }
    
    //Watch the pipe for incoming messages and populate the in-queue
    private void pipe_in_thread_proc() throws FileNotFoundException, IOException, InterruptedException{
        ThreadState state = ThreadState.RUN;
        
        while(state != ThreadState.EXIT){                                    
            //Keep attempting to read and queue messages until an interrupt occurs, then exit
            try{
                state = pipe_in_message();
            }
            catch(InterruptedException ex){
                m_input_parser.close();
                m_input_parser = null;
                m_in_stream.close();
                m_in_stream = null;
            }
            catch(Exception ex){
                Service.instance.log("Exception reading service pipe", null, ex);
                thread_state_lock.lock();
                m_in_state = ThreadState.PARKED;
                thread_state_lock.unlock();
                reset();
            }
        }
    }
    
    private void pipe_out_message() throws InterruptedException{
        m_out_queue_semaphore.acquire();
       // if(m_out_stream)
       // m_out_queue.take().toJson().build().
    }
    
    //Watch the output queue for messages and transmit them out the pipe
    private void pipe_out_thread_proc() throws InterruptedException{
        while(true){
            
            
        }
    }
    
}
*/