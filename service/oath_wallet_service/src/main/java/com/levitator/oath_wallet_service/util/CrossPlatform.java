package com.levitator.oath_wallet_service.util;

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
    static public final String operating_system_name = System.getProperty("os.name");
    
    static private OS get_os(){
        var osstr = operating_system_name.toUpperCase();
        
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
                throw new RuntimeException("Don't know how to make a fifo file on operating system named: " + operating_system_name);                
        }
        
    }
    
}
