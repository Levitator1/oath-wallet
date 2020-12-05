
package com.levitator.oath_wallet_service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * Well, it seems that Java's native Channel-based locking idiom doesn't work on Linux.
 * Or something. Who knows. So, here we implement our own inter-process lock system.
 * We use the existence of a file as our lock condition. However, that poses the problem
 * of how to detect stale locks, since Java doesn't provide any mechanism that I know of
 * for enumerating running processes. However! We can open a listen-socket and write
 * the port number to the file. Then, if we are able to query the port for the name of the lock file,
 * then we know that the lock is live and not stale. Also, to recover a stale lock, we lock the lock.
 * We could have n-level stale lock files. In practice, a second stale lock file should be very rare.
 * 
 */
public class IPCLock implements AutoCloseable {
    
    static private final byte[] local_ip_bytes = new byte[]{ 127, 0, 0, 1 };    
    
    File m_path;
    LockService m_service; 
    
    // We launch a service on the lock port so that we can distinguish the case where
    // the lock is held from when some other random application takes the port
    class LockService extends Thread implements AutoCloseable{

        private ServerSocket sock;
        public final int port;
        
        LockService() throws IOException{
            sock = open_service_port(0); //OS picks an available public port
            port = sock.getLocalPort();
        }
        
        private void handle_client(Socket client) throws IOException{
            try(var writer = new PrintWriter( client.getOutputStream() )){
                writer.write(m_path.toString() + "\n");
            }
        }
        
        private void thread_proc(){
            
            //Poll for exit/interrupt every 2 seconds 
            try{ sock.setSoTimeout(2000); }
            catch(Exception ex){}
            
            while(true){
                try{
                    try(var client = sock.accept()){
                        if(interrupted())
                            throw new InterruptedException();
                            
                        handle_client(client);
                    }
                }
                catch(InterruptedIOException ex){
                    return;
                }
                catch(Exception ex){
                    if(Thread.interrupted())
                        return;
                    else
                        Service.instance.log("Unexpected exception on lock maintenance thread", null, ex);
                }
            }
        }
        
        @Override
        public void run() {            
            thread_proc(); 
        }        

        @Override
        public void close() throws InterruptedException {
            this.interrupt();
            if(Thread.currentThread() != this)
                join();
        }
    }
    
    private int retrieve_lock_port() throws FileNotFoundException, IOException{
        byte[] data;
        try(var in = new FileInputStream(m_path)){
            data = in.readAllBytes();
        }                
        
        //This should be pretty unlikely. If the line is unterminated, then
        //we somehow caught the lock file in the middle of being written
        if(data[data.length-1] != '\n')
            throw new IOException("Lock file write was incomplete!");
        
        return Integer.parseInt( data.toString() );
    }
    
    //If we fail twice, then declare the lock both stale and half-written
    private int stubbornly_retrieve_lock_port() throws InterruptedException, IOException{
        
        int port;
        
        try{
            port = retrieve_lock_port();
        }
        catch(IOException ex){            
            //Highly improbable that we catch a lock file in the middle of being written
            //and then it still takes three seconds to finish. It's like a half dozen bytes
            Thread.sleep(3000); //Wait three seconds and hope the file write completes
            port = retrieve_lock_port();
        }
        return port;
    }
    
    //bind to localhost so that other machines can't connect.    
    ServerSocket open_service_port(int port) throws UnknownHostException, IOException{
        var result = new ServerSocket(port, 0, InetAddress.getByAddress(local_ip_bytes));
        result.setReuseAddress(true);                
        return result;                
    }        
    
    //true if the socket number is found in the lock file and it is active and valid
    //false if the validity of the lock file cannot be confirmed
    private boolean socket_check() throws FileNotFoundException, IOException{
        
        int port;
        try{
            port = stubbornly_retrieve_lock_port();            
        }
        catch(Exception ex){
            return false;
        }
        
        try(var sock = new Socket(InetAddress.getByAddress(local_ip_bytes), port)){            
            sock.setSoTimeout(2000);
            var stream = sock.getInputStream();
            var reader = new BufferedReader( new InputStreamReader(stream));
            var data = reader.readLine();
            return data.equals(m_path.toString());
        }
        catch(IOException ex){
            return false;
        }        
    }
    
    private File generate_recursive_lock_path(){
        var path = m_path.toPath();
        String filename = m_path.getName(), newname;        
        
        int dotpos = filename.lastIndexOf('.', Integer.MAX_VALUE);
        if(dotpos < 0)
            newname = filename + ".0";            
        else{
            var ordinal = Integer.parseInt( filename.substring(dotpos+1) );
            ++ordinal;
            newname = filename + "." + ordinal;
        }   
        return path.getParent().resolve(newname).toFile();
    }
    
    //If stale, returns a lock which can be used to reclaim this lock
    //If not stale throws an exception to tell the user that the lock is taken and valid
    private IPCLock check_for_staleness() throws LockException, IOException, InterruptedException{
        if(!socket_check()){
            //Hey, dawg, I herd u like to lock ur locks
            return new IPCLock( generate_recursive_lock_path());
        }
        else
            throw new LockException();
    }        
    
    private void setup_lock_impl(File path) throws IOException, LockException, InterruptedException{
        
        var m_path = path;
        IPCLock recovery_lock = null;
        
        //Javadocs say this is atomic and then immediately says not to use this for file locking
        //and then neglects to explain why not.
        //I figure it can't be worse than FileLock, which doesn't work at all. And anyway, double-opening
        //the app is probably not catastrophic, and the logistics of operating two GUIs simultaneously so as to corrupt
        //a common config file are rather implausible. Hopefully faults won't happen at all, especially with the socket-based exclusivity.       
        var success = path.createNewFile();
        
        if(!success)
            recovery_lock = check_for_staleness(); //throws if existing lock is valid and not stale                          
        
        //We either acquired the file at this point, or deterimned that the lock is stale
        try(var recovery_cleanup = recovery_lock){ //recovery lock may or may not be null
            m_service = new LockService();
            try(var out = new PrintStream(path)){
                out.println( m_service.port );
            }
        }
        catch(Exception ex){
            close();
            throw ex;
        }
    }
    
    public IPCLock(File path) throws LockException, IOException, InterruptedException{        
        setup_lock_impl(path);        
    }

    @Override
    public void close() throws InterruptedException {        
        m_path.delete();
                
        if(m_service != null)
            m_service.close();
    }
}
