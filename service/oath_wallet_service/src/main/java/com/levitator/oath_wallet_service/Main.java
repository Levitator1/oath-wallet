package com.levitator.oath_wallet_service;
import java.lang.System;
        
public final class Main {
    public static void main(String[] args) throws Exception {

        Service service = new Service(args);
        System.exit(service.run());
    }
}
