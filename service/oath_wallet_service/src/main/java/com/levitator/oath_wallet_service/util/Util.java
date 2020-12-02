
package com.levitator.oath_wallet_service.util;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Locale;

public class Util {
    
       static public void stack_trace(Throwable ex, PrintStream out, int indent){
        try{
            var elements = ex.getStackTrace();
            
            out.println("Stack trace: " + ex.getClass().getName() + ": " + ex.getMessage());
            for(var el : elements){
                for(int i=0;i<indent;++i){
                    out.print('\t');
                }
                out.println(el.toString());
            }
        }
        catch(Exception ex2){
            //Fail silently
        }
    }
    
    static public void stack_traces(Throwable ex, PrintStream out){
        try{
            stack_trace(ex, out, 0);
            var level=1;
            while((ex = ex.getCause()) != null){
                stack_trace(ex, out, level++);
            }
            
        }
        catch(Exception ex2){
            //Fail silently
        }
    }
    
    static public String full_stack_trace_string(Throwable ex){
        var byte_stream = new ByteArrayOutputStream();
        var stream = new PrintStream(byte_stream);
        stack_traces(ex, stream);
        stream.flush();
        return byte_stream.toString();
    }
    
}
