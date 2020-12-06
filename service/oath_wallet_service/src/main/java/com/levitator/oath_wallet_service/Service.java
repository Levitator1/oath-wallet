package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.messages.out.Bye;
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
import com.levitator.oath_wallet_service.messages.in.HiHowAreYou;
import com.levitator.oath_wallet_service.messages.in.InMessage;
import com.levitator.oath_wallet_service.messages.in.PINRequest;
import com.levitator.oath_wallet_service.messages.in.QuitMessage;
import com.levitator.oath_wallet_service.messages.out.ErrorMessage;
import com.levitator.oath_wallet_service.messages.out.Notification;
import com.levitator.oath_wallet_service.messages.out.OutMessage;
import com.levitator.oath_wallet_service.util.CrossPlatform;
import com.levitator.oath_wallet_service.util.NioFileInputStream;
import com.levitator.oath_wallet_service.util.NioFileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.List;
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
    InputStream m_in_stream;
    OutputStream m_out_stream;
    JsonParser m_parser;    

    public DomainMapper mapper() {
        return m_mapper;
    }
    
    static class MessageRegister{
        
        
        MessageRegister(){
            var classes = List.of(HiHowAreYou.class, PINRequest.class, QuitMessage.class, Bye.class);
            MessageFactory.instance().add( classes );                        
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
       //var bufstream = new StringWriter();
       var bufstream = new ByteArrayOutputStream();                     
       var writer = Json.createWriter(bufstream);      
       writer.write(msg.toJson().build());
       writer.close();
       
        //Stupid undocumented MDN crap
        final ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
        buf.order(ByteOrder.nativeOrder());
        buf.putInt(bufstream.size());
        out.write( buf.array() );      
        bufstream.writeTo(out);                
        out.flush();
    }
    
    public void send_to_client(OutMessage msg) throws IOException{
        send_message(msg, m_out_stream);
    }
    
    public void error_to_client(String message, long session) throws IOException{
        var packet = new ErrorMessage(message, session);
        send_to_client(packet);
    }
    
    public void notice_to_client(String message, long session) throws IOException{
        var packet = new Notification(message, session);
        send_to_client(packet);
    }
    
    //Used to find the next element in the message array, whether it's an object or array close
    private void require_parse_next() throws GeneralInterruptException, IOException, EOFException{
        if( !m_parser.hasNext() ){
            log("Warning: client disconnected unexpectedly");
            //reset_io(); //Excessive complexity. Throw an error and iterate from the beginning
            throw new EOFException("EOF before message array close");
        }
    }
    
    private void process_message() throws InterruptedException, EOFException, GeneralInterruptException, CleanEOF, IOException{
        
        reset_io();
        require_parse_next(); //Demand that there be a next json element
        var next = m_parser.next();
        if(next == Event.END_ARRAY){ //It can be an array end
            log("Client disconnect\n");            
            return;
        }        
        
        if(next != Event.START_OBJECT) //or an object start
            throw new JsonParsingException ("Expected start of JSON object", m_parser.getLocation());                
        
        //Failing this exits the program, but maybe that's a failsafe thing to do
        //if someone is sending unintelligible messages (in this case with bad type() strings)
        var obj = (InMessage)MessageFactory.instance().create(m_parser);
        
        obj.process(this);          
    }
    
    static private void check_interrupt() throws GeneralInterruptException{
        if(Thread.interrupted())
            throw new GeneralInterruptException(new InterruptedException());
    }
    
    private void reset_io() throws FileNotFoundException, GeneralInterruptException, CleanEOF, IOException, InterruptedException{

        if(m_parser != null)
            m_parser.close();
        
        if(m_in_stream != null)
            m_in_stream.close();
                      
        if(m_out_stream != null){
            try{
                send_to_client(new Bye());
            }
            catch(Exception ex){
                log("Warning: failed sending client disconnect request", null, ex);
            }
            m_out_stream.close();
        }
        
        //The file-open may block if the other side of the pipe is not yet connected
        //and it seems to do so non-interruptibly
        check_interrupt();
        //On exit, this gets unblocked by a file-open so that we are awake and can see the interrupt
        //NIO is supposed to be fully interruptible, but I guess Open JDK didn't account for the one-sided fifo case
        //Also, the two fifo opens have to be in the same order (client/server), or the operation will probably deadlock
        
        //Bizarre timing issue here. It seems that if you close a Linux fifo and then reopen it really quickly, the
        //other end never sees the disconnect. We mitigate this by implementing a "Bye" message which represents a
        //request for the browser to disconnect on its end, thus making connection termination explicit and better-defined.
        Thread.sleep(1000);
        m_in_stream = new NioFileInputStream(Config.instance.fifo_in_path.toFile());
        m_out_stream = new NioFileOutputStream(Config.instance.fifo_out_path.toFile());
        check_interrupt();
        
        //Looks like each message from the browser is a Pascal string with a 4-byte header representing the string length
        //in little endian... so, machine byte order?. It would be nice if the MDN manuals mentioned that. Maybe it turns
        //out to be a lucky thing since the json parser seems to be hanging reading off the end of the available data.
        var lenbytes = ByteBuffer.wrap( new byte[4] );
        lenbytes.order(ByteOrder.nativeOrder());
        m_in_stream.read(lenbytes.array(), lenbytes.arrayOffset(), 4);
        var strlen = lenbytes.getInt();
        
        
        byte[] buf = new byte[strlen];
        m_in_stream.read(buf);
        var current_message = new ByteArrayInputStream(buf);
        
//        try(var in = new InputStreamReader(m_in_stream)){
//            var ch = in.read();
//            
//            if(ch != '!' && ch != 'Y'){
//                log("ERROR: Was expecting Mozilla protocol to include single-character prefix. Communication will probably fail.");
//            }                
//        }
        
        m_parser = Json.createParser(current_message);        
        
        //We will treat the incoming connection as a streaming array of json objects
        //Because if you complete a top-level object, the parser expects to see EOF immediately
        if(m_parser.next() != Event.START_ARRAY)
            throw new JsonParsingException("Service input must be at an object array at the top level", m_parser.getLocation());        
    }
    
    //We need the same fat error handler twice, so here is a lamba for those function bodies
    @FunctionalInterface
    static interface MessageHandlingTask{
        public void run() throws GeneralInterruptException, InterruptedException, EOFException, CleanEOF, IOException;
    }
    
    private void message_loop_error_handler(MessageHandlingTask f) throws GeneralInterruptException{
        try{
            f.run();
        }
        catch(JsonException ex){
            //Ok, so because we hacked together an interruptible NIO-based implemention of the streams, as a result, some Json
            //errors are due to interrupts, but we have to search the causal chain to find out which, so that we
            //can exit cleanly for those instead of logging or reporting them as errors
            ClosedByInterruptException cause = Util.search_causes(ClosedByInterruptException.class, ex);
            if( cause != null)
                throw new GeneralInterruptException(cause); //cleanish exit. We might have been in the middle of something, but were told to quit.
            else{
                log("json parse error. Resetting connection.", null, ex);
            }
        }
        catch(InterruptedException ex){
            throw new GeneralInterruptException(ex);
        }
        catch(InterruptedIOException ex){
            throw new GeneralInterruptException(ex); //clean exit
        }
        catch(CleanEOF ex){
            //Do nothing/proceed to next message
        }
        catch(EOFException ex){
            log("Unexpected end of message", null, ex);
        }
        catch(IOException ex){
            log("IO error", null, ex);
        }
        catch(Exception ex){
            //Report an unexpected error, which we recover from by resetting the IO state
            log("Unexpected error processing service IO", null, ex);
        }        
    }
    
    private void message_loop() throws GeneralInterruptException, FileNotFoundException, IOException{                                   

        //Don't need this
        //message_loop_error_handler( ()->{ reset_io(); } );        
        set_status_text("Status: READY");
        
        while(Main.exit_code() == null){
           message_loop_error_handler( ()->{ process_message(); } );
        }
        
        //We don't need to be interrupted anymore because we are awake and exiting
        Thread.interrupted(); //clears the flag
    }
   
    private void do_run() throws InterruptedException, IOException, ExecutionException{
        
        try{
            m_mapper = new DomainMapper();            
        }        
        catch( LockException ex ){
            m_mapper = null;            
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
            log("Existing instance detected. Running in relay mode.");
            try{
                var pipe_mode = new PipeMode();
                pipe_mode.same_thread_run();
            }
            catch(Exception ex){
                log("IO error", null, ex);                
            }
            return;
        }
        
        //This launches another thread for all the GUI stuff and then wastefully blocks the current thread
        //So, we launch a thread to launch the GUI thread so that it gets blocked instead
        //Application.launch(FXApp.class, m_args);
        m_gui_thread = new Thread( () -> { Application.launch(FXApp.class, m_args);  }, "GUI Launch Thread" );
        m_gui_thread.start();                
        m_gui = FXApp.last_app_started();
                       
        //Now we are free to do background stuff on a thread independent of the GUI
        log("");
        var text = Config.instance.app_title + " " + "started";        
        log(text, Config.instance.console_bold_font);
        log(IntStream.range(0, text.length()).mapToObj(i -> "=").collect(Collectors.joining("")), Config.instance.console_bold_font);
        log("" + m_mapper.mappings().size() + " credential entries loaded from: " + Config.instance.domain_config);
        log("");
        
        /*
        var pipe_mode_thread = new Thread( ()->{ 
            var pipe_mode = new PipeMode();
            pipe_mode.run();
        });
        */
        
        try{
            //Create the FIFOs for client instances to talk to us (the server/gui instance)            
            mkfifo( Config.instance.fifo_in_path );
            mkfifo( Config.instance.fifo_out_path );
            
            //Create a relay thread to handle the first communication session
            //This allows the stdio interface to be the same regardless of whether this instance
            //is the first, or a subsequent client instance
            try(var initial_relay = new PipeMode()){
                initial_relay.start();
                message_loop();            
            }
        }
        catch(GeneralInterruptException ex){
            if(Main.exit_code() == null){
                log("Interrupt ocurred. Exiting...", null, ex);                                                
                return;
            }            
        }
        catch(Exception ex){
            log("Unexpected error. Exiting...", null, ex);            
            return;
        }
        log("Program exiting normally");
    }
    
    private void mkfifo(Path path) throws IOException, InterruptedException{
        if( !path.toFile().exists() ){                         
            var result = CrossPlatform.mkfifo( path );
            if(result != 0){
                log("\"Unable to create IPC fifo: '\" + Config.instance.fifo_path + \"' Result code: \" + result");
                log("If you are running Linux or an operating system that permits it, you should be able to create this fifo manually and try again.");
                throw new RuntimeException("Failed creating FIFO");
            }
        }
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
        
        //Open the FIFOs as if a client were connected because otherwise
        //our own server-side open operation may be non-interruptibly blocked waiting for a connection
        try{
            try(var out = new FileOutputStream(Config.instance.fifo_in_path.toFile()); var in = new FileInputStream(Config.instance.fifo_out_path.toFile())){}
            //m_in_stream.close();
            service_thread.interrupt();            
        }
        catch(Exception ex){}
    } 
}
