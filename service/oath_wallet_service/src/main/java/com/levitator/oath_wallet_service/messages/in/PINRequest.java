
package com.levitator.oath_wallet_service.messages.in;

import com.levitator.oath_wallet_service.Parser;
import com.levitator.oath_wallet_service.Service;
import javax.json.stream.JsonParser;

public class PINRequest implements InMessage {
    
    private long m_session_id;
    private String m_url;    
    
    @Override
    public void process(Service serv) {
        serv.log("Woots: URL: " + url());
    }

    @Override
    public String type() {
        return "pin_request";
    }

    @Override
    public void parse(JsonParser parser) {
        m_session_id = Parser.demand_long_named(parser, "session");
        m_url = Parser.demand_string_named(parser, "url");
        Parser.demand_end_object(parser);
    }

    private String url() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
