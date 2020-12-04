package com.levitator.oath_wallet_service;

/**
 *
 * Some subprocess which returns fairly short messages in the output streams.
 * We return stdout on success or throw stderr on failure
 * 
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class StringReturnProcess{
    
    private ProcessBuilder m_builder;
    
    public StringReturnProcess(String... args){                
        m_builder = new ProcessBuilder(args);         
    }
    
    public String run() throws IOException, InterruptedException, ExecutionException, YkmanExitCodeException, ProcessReturnCodeException{
        var process = m_builder.start();
        var stdout_data = new ByteArrayOutputStream();
        var stderr_data = new ByteArrayOutputStream();
        var buf = new byte[256];
        int count=0, result;
        
        do{
            result = process.getInputStream().read( buf );
            if(result >= 0){
                stdout_data.write(buf, 0, result);
                count = result;
            }
            else
                count = 0;
            
            result = process.getErrorStream().read( buf );            
            if(result > 0){
                stderr_data.write(buf, 0, result);
                count += result;
            }                                    
        }while(count > 0);
        
        if(process.isAlive())
            process.onExit().get();                
        
        if(process.exitValue() == 0){
           return stdout_data.toString();
        }
        else{
            var stderr_str = stderr_data.toString();
            throw new ProcessReturnCodeException(stderr_str, process.exitValue());
        }       
    }
}
