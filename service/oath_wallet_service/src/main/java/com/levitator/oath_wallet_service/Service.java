package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.ui.jfx.FXApp;
import com.levitator.oath_wallet_service.util.Util;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.application.Application;
import javafx.scene.text.Font;
import javax.swing.JOptionPane;

public class Service{
    
    static public final Service instance = new Service();
    private String[] m_args;
    private FXApp m_gui;
    private Thread m_gui_thread;
    private DomainMapper m_mapper;
    
    private Service(){        
    }
    
    private void do_run() throws InterruptedException, IOException, ExecutionException{        
        //This launches another thread for all the GUI stuff and then wastefully blocks the current thread
        //So, we launch a thread to launch the GUI thread so that it gets blocked instead
        //Application.launch(FXApp.class, m_args);
        m_gui_thread = new Thread( () -> { Application.launch(FXApp.class, m_args);  } );
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
            log("Could not read domain/credential mappings from: " + Config.instance.domain_config, null, ex);
            log("You have no credentials configured. You will need to exit the program and fix the problem.");
        }
        
        set_status_text("Status: READY");
        while(m_gui_thread.isAlive()){
            Thread.sleep(1);
        }
        
        Service.instance.log("Main thread exiting");
    }
    
    public void run(String[] args) throws InterruptedException, ExecutionException, IOException{
        m_args=args;
        
        try{
            do_run();
        }
        catch(Exception x){
            Main.exit(-1);
            throw x;
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
    public void log(String msg, Font font, Exception ex){
        
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
    
}
