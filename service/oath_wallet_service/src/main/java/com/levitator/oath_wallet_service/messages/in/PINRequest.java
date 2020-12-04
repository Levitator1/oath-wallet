
package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Parser;
import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.YkmanExitCodeException;
import com.levitator.oath_wallet_service.YkmanProcess;
import com.levitator.oath_wallet_service.messages.out.PINReply;
import java.io.IOException;
import static java.util.Map.entry;
import javax.json.stream.JsonParser;

public class PINRequest extends InMessage {
    
    private String m_url;    
    
    public PINRequest(){        
    }
    
    @Override
    public void process(Service serv) throws IOException {
        var mappings = serv.mapper().map(url());
        if(mappings.size() < 1){
            serv.error_to_client("Could not find any matching credentials for URL: " + url(), session_id());
        }
        else if( mappings.size() > 1 ){
            var msg = "URL '" + url()  + "' is ambiguous because it matches more than one credential: ";
            for(var entry : mappings.subList(1, mappings.size())){
                msg = msg + entry.cred() + ", ";
            }
            msg = msg + mappings.get( mappings.size() - 1 ).cred();
            serv.error_to_client(msg, session_id());
        }
        else{
            //Successful credential match                                   
            try{
                var mapping = mappings.get(0);
                var ykman = new YkmanProcess("oath", "code", "-s", mapping.cred());
                var pin = ykman.run(); //TODO: make sure the result is actually all digits
                var msg = new PINReply(session_id(), url(), mapping.cred(), pin);
                serv.send_to_client(msg);
            }
            catch(YkmanExitCodeException ex){
                //No need for a stack trace, as this error is really specific
                serv.log(ex.getMessage());
                serv.error_to_client(ex.getMessage(), session_id());
            }
            catch(Exception ex){
                //serv.log("Working directory: " + System.getProperty("user.dir") );
                var msg = "Error executing or talking to 'ykman' command-line tool";
                serv.log(msg, null, ex);
                serv.error_to_client(msg + ": " + ex.getMessage(), session_id());
            }            
        }        
    }

    @Override
    public String type() {
        return "pin_request";
    }

    @Override
    public void parse(JsonParser parser) {
        super.parse(parser);
        m_url = Parser.demand_string_named(parser, "url");        
    }

    public String url() {
        return m_url;
    }        
}
