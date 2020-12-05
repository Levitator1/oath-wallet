package com.levitator.oath_wallet_service.util;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {    
    public void accept(T v) throws E;    
}
