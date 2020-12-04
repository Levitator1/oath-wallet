package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.ui.jfx.FXApp;
import com.levitator.oath_wallet_service.util.Util;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javax.swing.JOptionPane;
import com.levitator.oath_wallet_service.messages.common.MessageFactory;
import com.levitator.oath_wallet_service.messages.in.InMessage;
import com.levitator.oath_wallet_service.messages.in.PINRequest;
import com.levitator.oath_wallet_service.messages.in.QuitMessage;
import com.levitator.oath_wallet_service.messages.out.ErrorMessage;
import com.levitator.oath_wallet_service.messages.out.OutMessage;
import com.levitator.oath_wallet_service.util.CrossPlatform;
import com.levitator.oath_wallet_service.util.NioFileInputStream;
import com.levitator.oath_wallet_service.util.NioFileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.ClosedByInterruptException;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

public class Service{
    
    static public final Service instance = new Service();
    private String[] m_args;
    private FXApp m_gui;
    private Thread m_gui_thread;
    private DomainMapper m_mapper;
    private Thread service_thread;

    //Message loop stuff
    NioFileInputStream m_in_stream;
    NioFileOutputStream m_out_stream;
    JsonParser m_parser;    

    public DomainMapper mapper() {
        return m_mapper;
    }
    
    static class MessageRegister{
        MessageRegister(){
            MessageFactory.instance().add("pin_request", PINRequest.class);
            MessageFactory.instance().add("quit", QuitMessage.class);
        }
    }
    
    private static final MessageRegister m_register_in_messages = new MessageRegister();
    
    //Construct this on the main the thread
    private Service(){
        service_thread = Thread.currentThread();
    }        
    
    static public void send_message( OutMessage msg, OutputStream out) throws IOException{
        
        //Looks like we have to buffer these objects one at a time because a given parser
        //seems to expect a single document root
       var bufstream = new ByteArrayOutputStream();
       var writer = Json.createWriter(bufstream);        
       writer.write(msg.toJson().build());
       writer.close();
       bufstream.writeTo(out);
       out.write('\n'); //Makes the protocol a bit easier to raed
       out.flush();
    }
    
    public void send_to_client(OutMessage msg) throws IOException{
        send_message(msg, m_out_stream);
    }
    
    public void error_to_client(String message, long session) throws IOException{
        var packet = new ErrorMessage(message, session);
        send_to_client(packet);
    }
    
    private void process_message() throws InterruptedException, Exception{
        
        if( !m_parser.hasNext() ){
            log("Client disconnect");
            reset_io();
        }
            
        if(m_parser.next() != Event.START_OBJECT)
            throw new JsonParsingException ("Expected start of JSON object", m_parser.getLocation());
        
        var obj = (InMessage)MessageFactory.instance().create(m_parser);              
        obj.process(this);        
    }
    
    private void check_interrupt() throws GeneralInterruptException{
        if(Thread.interrupted())
            throw new GeneralInterruptException(new InterruptedException());
    }
    
    private void reset_io() throws FileNotFoundException, GeneralInterruptException, IOException{

        if(m_parser != null)
            m_parser.close();
        
        if(m_in_stream != null)
            m_in_stream.close();
                      
        if(m_out_stream != null)
            m_out_stream.close();
        
        //The file-open may block if the other side of the pipe is not yet connected
        //and it seems to do so non-interruptibly
        check_interrupt();
        //On exit, this gets unblocked by a file-open so that we are awake and can see the interrupt
        //NIO is supposed to be fully interruptible, but I guess Open JDK didn't account for the one-sided fifo case
        m_in_stream = new NioFileInputStream(Config.instance.fifo_path.toFile());
        m_out_stream = new NioFileOutputStream(Config.instance.fifo_path.toFile());
        check_interrupt();
        m_parser = Json.createParser(m_in_stream);        
        
        //We will treat the incoming connection as a streaming array of json objects
        //Because if you read a top-level object, the parser expects to see EOF immediately
        if(m_parser.next() != Event.START_ARRAY)
            throw new JsonParsingException("Service input must be at an object array at the top level", m_parser.getLocation());        
    }
    
    private void message_loop() throws GeneralInterruptException, FileNotFoundException, IOException{                                   

        reset_io();
        
        set_status_text("Status: READY");
        
        while(Main.exit_code() == null){
            try{
                process_message();
            }
            catch(JsonException ex){
                //Ok, so because we hacked together an interruptible NIO-based implemention of the streams, as a result, some Json
                //errors are due to interrupts, but we have to search the cause chain to find out which, so that we
                //can exit cleanly for those instead of logging or reporting them as errors
                ClosedByInterruptException cause = Util.search_causes(ClosedByInterruptException.class, ex);
                if( cause != null)
                    throw new GeneralInterruptException(cause); //clean exit
            }
            catch(InterruptedException ex){
                throw new GeneralInterruptException(ex); //clean exit
            }
            catch(Exception ex){
                //Report an unexpected error, which we recover from by resetting the IO state
                log("Error processing service IO", null, ex);
                reset_io();
            }
        }
        
        //We don't need to be interrupted anymore because we are awake and exiting
        Thread.interrupted(); //clears the flag
    }
   
    private void do_run() throws InterruptedException, IOException, ExecutionException{
        
        try{
            m_mapper = new DomainMapper();            
        }        
        catch( ConfigLockedException ex ){
            m_mapper = null;
            return;
        }
        catch( Exception ex){
            //TODO: Since JFX offers no reasonable way to show popup notifications, we willl
            //need to delegate them to the Web browser. We could jury rig something, but then
            //it might conflict positionally and overlap with other notifications.
            log("Could not read domain/credential mappings from: " + Config.instance.domain_config, null, ex);
            log("You have no credentials configured. You will need to exit the program and fix the problem.");
        }
        
        //If the mapping file is already claimed, then there is already a process running,
        //so, let's run in pipe mode, as a relay
        if(m_mapper == null){
            try{
                var pipe_mode = new PipeMode();
                pipe_mode.run();
            }
            catch(Exception ex){
                
            }
        }
        
        //This launches another thread for all the GUI stuff and then wastefully blocks the current thread
        //So, we launch a thread to launch the GUI thread so that it gets blocked instead
        //Application.launch(FXApp.class, m_args);
        m_gui_thread = new Thread( () -> { Application.launch(FXApp.class, m_args);  }, "GUI Launch Thread" );
        m_gui_thread.start();                
        m_gui = FXApp.last_app_started();
                       
        //Now we are free to do background stuff on a thread independent of the GUI
        var text = Config.instance.app_title + " " + "started";        
        log(text, Config.instance.console_bold_font);
        log(IntStream.range(0, text.length()).mapToObj(i -> "=").collect(Collectors.joining("")), Config.instance.console_bold_font);
        log("" + m_mapper.mappings().size() + " credential entries loaded from: " + Config.instance.domain_config);
                       
        try{
            //Create the FIFO for client instances to talk to us (the server/gui instance)
            if( !Config.instance.fifo_path.toFile().exists() ){
                var result = CrossPlatform.mkfifo( Config.instance.fifo_path );
                if(result != 0){
                    log("\"Unable to create IPC fifo: '\" + Config.instance.fifo_path + \"' Result code: \" + result");
                    log("If you are running Linux or an operating system that permits it, you should be able to create this fifo manually and try again.");
                    throw new RuntimeException("Failed creating FIFO");
                }
            }
            
            message_loop();            
        }
        catch(GeneralInterruptException ex){
            if(Main.exit_code() == null){
                log("Unexpected interrupt ocurred. Exiting...", null, ex);                                                
                return;
            }            
        }
        catch(Exception ex){
            log("Unexpected error. Exiting...", null, ex);            
            return;
        }
        log("Program exiting normally");
    }
    
    private void gui_shutdown(){
        
        if(m_gui_thread == null)
            return;
        
        //This causes the GUI thread to exit its implicit event loop and terminate
        Platform.exit();
 
        //Apply to the GUI a 10-second timeout to exit in case something unexpected is wrong
        try { m_gui_thread.join(1000 * 10); }
        catch(Exception ex){}
        
        if(m_gui_thread.isAlive())
            log("Gave up waiting on stalled GUI thread");
        else
            log("GUI thread exited normally.");
    }
    
    public void run(String[] args) throws InterruptedException, ExecutionException, IOException{
        m_args=args;
        
        try{
            do_run();
        }
        finally{
            if(m_mapper != null) 
                m_mapper.close();

            if(m_parser != null)
                m_parser.close();
            
            if(m_in_stream != null)
                m_in_stream.close();
            
            gui_shutdown();
                
            //GUI thread seems to die when the main thread exits.
            //No need to rethrow from here because our messages are written, the error code is set,
            //and we either killed the GUI thread or gave up waiting for it  
        }        
    }
    
    public void set_status_text(String text){
        m_gui.set_status_text(text);
    }
    
    //Log informative messages
    public synchronized void log(String msg, Font font, Exception ex){
        
        //Let's send the stack trace solely to stderr because it makes the console
        //popup pretty unreadable
        var long_msg = make_error_message(msg, ex);
        System.err.println(long_msg);
        
        try{
            if(m_gui != null && m_gui_thread != null && m_gui_thread.isAlive())
                m_gui.write_console_text(msg + "\n", font);
        }
        catch(Exception ex2){
            //Should happen very rarely if ever. Might have a slight race condition between when
            //the GUI thread exits and the main thread notices
            System.err.print("Warning: print to console window failed: " + ex2.getMessage());
        }
    }
    
    public void log(String msg, Font font){
        log(msg, font, null);
    }
    
    public void log(String msg){
        log(msg, Config.instance.console_default_font, null);
    }
    
    //Respond to fatal errors making no assumptions about program state
    static public void fatal(String msg, Exception ex){        
        msg = make_error_message(msg, ex);
        System.err.print(msg);
        JOptionPane.showMessageDialog(null, msg,
            Config.instance.app_name, JOptionPane.ERROR_MESSAGE);            
    }
    
    public static void fatal(String msg){
        fatal(msg, null);
    }
    
    static public String make_error_message(String desc, Exception ex){
        if(ex != null)
            desc = desc + "\nStack trace: " + Util.full_stack_trace_string(ex);
        else
            desc = desc + "\n";
        
        return desc;
    }

    public void exit(int i){
        
        Main.exit_code(i); //This sets the exit code for when main() returns
                
        //Everything below is dedicated to unblocking the main thread so that
        //it notices it's time to exit                
        
        //Open the FIFO for writing as if a client were connected because otherwise
        //our own serve-side open operation may be non-interruptibly blocked waiting for a connection
        try{
            try(var out = new FileOutputStream(Config.instance.fifo_path.toFile())){}
            //m_in_stream.close();
            service_thread.interrupt();            
        }
        catch(Exception ex){}
    } 
}
