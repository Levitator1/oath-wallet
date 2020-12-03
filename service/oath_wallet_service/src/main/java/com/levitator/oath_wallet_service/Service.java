package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.ui.jfx.FXApp;
import com.levitator.oath_wallet_service.util.Util;
import java.io.IOException;
import java.nio.channels.FileLockInterruptionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedTransferQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javax.swing.JOptionPane;
import com.levitator.oath_wallet_service.messages.common.IMessage;
import com.levitator.oath_wallet_service.messages.common.MessageFactory;
import com.levitator.oath_wallet_service.util.CrossPlatform;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Semaphore;
import javax.json.Json;
import javax.json.JsonWriter;
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

    
    //Construct this on the main the thread
    private Service(){
        service_thread = Thread.currentThread();
    }        
    
    private void message_loop() throws GeneralInterruptException, FileNotFoundException, IOException{                                   
        
        set_status_text("Status: READY");
                
        //Probably won't need this, but it's for completeness to catch all the possible
        //ways an interrupt event can be thrown
        //catch(FileLockInterruptionException ex){
        //    throw new GeneralInterruptException(ex);
        //}
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
        
        //Create the FIFO for client instances to talk to us (the server/gui instance)
        if( !Config.instance.fifo_path.toFile().exists() ){
            var result = CrossPlatform.mkfifo( Config.instance.fifo_path );
            if(result != 0){
                log("\"Unable to create IPC fifo: '\" + Config.instance.fifo_path + \"' Result code: \" + result");
                log("If you are running Linux or an operating system that permits it, you should be able to create this fifo manually and try again.");
                throw new RuntimeException("Failed creating FIFO");
            }
        }
        
        try{
            message_loop();            
        }
        catch(GeneralInterruptException ex){
            if(Main.exit_code() == null){
                log("Unexpected interrupt ocurred. Exiting...", null, ex);                
                exit(-1); //Tells the GUI thread to exit and sets Main's return code
                
                //Discard the interrupted status we just set because we don't need to be interrupted again
                Thread.interrupted(); 
                
                //Apply to the GUI a 10-second timeout to exit since something unknown is wrong
                m_gui_thread.join(1000 * 10);
                if(m_gui_thread.isAlive())
                    log("Gave up waiting on stalled GUI thread");
                else
                    log("GUI thread exited normally.");
                
                //GUI thread seems to die when the main thread exits.
                //No need to rethrow from here because our messages are written, the error code is set,
                //and we either killed the GUI thread or gave up waiting for it
                return;
            }            
        }
        log("Program exiting normally");
    }
    
    public void run(String[] args) throws InterruptedException, ExecutionException, IOException{
        m_args=args;
        
        try{
            do_run();
        }
        finally{
            if(m_mapper != null) 
                m_mapper.close();
            
            if(m_gui_thread != null)
                m_gui_thread.join();
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

    public void exit(int i) {
        //This sets the exit code for when main() returns
        Main.exit_code(i);
        
        //This causes the GUI thread to exit its implicit event loop and terminate
        Platform.exit();
        
        //This causes the main/service thread to throw an exception,
        //join the GUI thread, and return
        service_thread.interrupt();
    }
}
