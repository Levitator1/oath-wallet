package com.levitator.oath_wallet_service.messages.common;

public abstract class SessionMessageBase implements SessionMessage{

    private long m_session_id;    //Mostly just implements this one state variable
    
    public SessionMessageBase(){
        session_id(0);
    }
    
    public SessionMessageBase(long session){
        session_id(session);
    }

    @Override
    public long session_id() {
        return m_session_id;
    }

    @Override
    public void session_id(long v) {
        m_session_id = v;
    }   
}
