package com.levitator.oath_wallet_service.ui;

import javafx.application.Application;

import javafx.stage.Stage;

// This is how we start JavaFX
public class FXApp extends Application{

    private String[] argv;
    private SystemTrayUI tray_ui;
    
    //FX demands a default constructor
    public FXApp(){}
    
    public FXApp(String[] args){
        argv = args;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        
        tray_ui = new SystemTrayUI();
        var test = new FXMLTest();
        test.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
}