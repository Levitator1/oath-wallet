package com.levitator.oath_wallet_service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class YkmanProcess extends StringReturnProcess{
    
    private ProcessBuilder m_builder;
    
    static private String[] prepend_program_to_args(String[] args){
        var args2 = new String[args.length + 1];
        args2[0] = Config.instance.ykman_path.toString();
        System.arraycopy(args, 0, args2, 1, args.length);
        return args2;
    }
    
    public YkmanProcess(String... args){                
        super(prepend_program_to_args(args));  
    }
    
    public String run() throws IOException, InterruptedException, ExecutionException, YkmanExitCodeException{
        try{
            return super.run();
        }
        catch(ProcessReturnCodeException ex){
            throw new YkmanExitCodeException(ex);
        }
    }
}
