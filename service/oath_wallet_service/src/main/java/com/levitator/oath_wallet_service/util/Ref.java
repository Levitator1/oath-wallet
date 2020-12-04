package com.levitator.oath_wallet_service.util;

/*
*
* Boxed reference for reference to reference
*
*/
public class Ref<T> {
    
    private T m_ref;
    
    public Ref(T ref){
        m_ref = ref;
    }
    
    T get(){
        return m_ref;
    }
    
    void set(T ref){
        m_ref = ref;
    }
    
}
