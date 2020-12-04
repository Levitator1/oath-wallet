package com.levitator.oath_wallet_service.util;

import com.levitator.oath_wallet_service.StringReturnProcess;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class CrossPlatform {

    public enum OS{
        UNKNOWN,
        LINUX,
        WINDOWS
    }
    
    static public final Runtime runtime = Runtime.getRuntime();
    static public final OS operating_system = get_os();    
    
    static public String get_os_name(){
        return System.getProperty("os.name");
    }
    
    static private OS get_os(){
        var osstr = get_os_name().toUpperCase();
        
        if(osstr.startsWith("WINDOWS"))
            return OS.WINDOWS;
        else if (osstr.startsWith("LINUX"))
            return OS.LINUX;
        else
            return OS.UNKNOWN;
    }
 
    //Return is the return code of the system/shell command issued to create the fifo
    //which is conventionally zero for success and something else for failure
    static public int mkfifo(Path path) throws IOException, InterruptedException{
        
        switch(operating_system){
            
            //TODO
            case WINDOWS:
                throw new RuntimeException("Named pipes are not implemented for Windows yet." + 
                        "If you are a coder, maybe you can figure out how to call the pipe APIs using JNI. Sorry." + 
                        "Also, note that Windows pipes are client/server like UNIX-domain sockets " +
                        "and unlike UNIX fifos, which behave more like a single stream");
                
            case LINUX:
                var cmd = new String[]{ "mkfifo", path.toString() };
                var process = runtime.exec(cmd);
                return process.waitFor();                
                            
            case UNKNOWN:
            default:
                throw new RuntimeException("Don't know how to make a fifo file on operating system named: " + get_os_name());                
        }
        
    }
 
    static public String auto_detect_command_path(String cmd){
        
        //falls back to the plain command name if it can't find a more specific path
        switch(CrossPlatform.operating_system){
            case LINUX:
                //Under Linux there is a standard command to find out where other commands come from
                //Also, bash is pretty much always in the same place
                var proc = new StringReturnProcess("/usr/bin/bash", "-l", "-c", "which $1", "which", cmd);
                
                try{
                    var path = proc.run();
                    if(path.endsWith("\n"))
                        return path.substring(0, path.length()-1);
                    else
                        return path;
                }
                catch(Exception ex){
                    return cmd;
                }                
                
            //TODO: Make this work if it doesn't already work
            case WINDOWS:
            default:
                return cmd;
        }
    }
    
    //The OS-specific path that user-specific configuration files go in
    //In linux this is ~ or $HOME
    //In Windows, this is somewhere else and I'm too lazy to go look it up, so TODO
    static public Path get_os_user_config_dir(){
        switch(operating_system){
            
            default:
            case LINUX:
                return Path.of(System.getenv("HOME"));
        }
    }
            
}
