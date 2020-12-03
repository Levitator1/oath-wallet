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
import com.levitator.oath_wallet_service.util.CrossPlatform;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.json.Json;
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
    FileInputStream in_stream;
    JsonParser in_parser;
    
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
    
    private void process_message() throws InterruptedException, Exception{
        
        if( !in_parser.hasNext() ){
            log("Client disconnect");
            reset_io();
        }
            
        if(in_parser.next() != Event.START_OBJECT)
            throw new JsonParsingException ("Expected start of JSON object", in_parser.getLocation());
        
        var obj = (InMessage)MessageFactory.instance().create(in_parser);
        obj.process(this);        
    }
    
    private void check_interrupt() throws GeneralInterruptException{
        if(Thread.interrupted())
            throw new GeneralInterruptException(new InterruptedException());
    }
    
    private void reset_io() throws FileNotFoundException, GeneralInterruptException{

        //The file-open may block if the other side of the pipe is not yet connected
        //and it seems to do so non-interruptibly
        check_interrupt();
        //On exit, this gets unblocked by a file-open so that we are awake and can see the interrupt
        in_stream = new FileInputStream(Config.instance.fifo_path.toFile());
        check_interrupt();

        in_parser = Json.createParser(in_stream);
    }
    
    private void message_loop() throws GeneralInterruptException, FileNotFoundException, IOException{                                   

        reset_io();
        
        set_status_text("Status: READY");
        
        while(Main.exit_code() == null){
            try{
                process_message();
            }
            catch(InterruptedException ex){
                throw new GeneralInterruptException(ex);
            }
            catch(Exception ex){
                log("Error processing service IO", null, ex);
                reset_io();
            }
        }
        
        //We don't need to be interrupted anymore because we are awake and exiting
        Thread.interrupted(); //clears the flag
    }
    
    private void do_run() throws InterruptedException, IOException, ExecutionException{        
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
        
        try{
            m_mapper = new DomainMapper();
            log("" + m_mapper.mappings().size() + " credential entries loaded from: " + Config.instance.domain_config);
        }        
        catch( ConfigLockedException ex ){
            //TODO
            throw new RuntimeException("Oops", ex);
        }
        catch( Exception ex){
            //TODO: Since JFX offers no reasonable way to show popup notifications, we willl
            //need to delegate them to the Web browser. We could jury rig something, but then
            //it might conflict positionally and overlap with other notifications.
            log("Could not read domain/credential mappings from: " + Config.instance.domain_config, null, ex);
            log("You have no credentials configured. You will need to exit the program and fix the problem.");
        }
                       
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

            if(in_parser != null)
                in_parser.close();
            
            if(in_stream != null)
                in_stream.close();
            
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
        System.err.print(long_msg);
        
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
        
        service_thread.interrupt();
        
        //Open the FIFO for writing as if a client were connected because otherwise
        //our own serve-side open operation may be non-interruptibly blocked waiting for a connection
        try{
            try(var out = new FileOutputStream(Config.instance.fifo_path.toFile())){}
        }
        catch(Exception ex){}
    }
}
