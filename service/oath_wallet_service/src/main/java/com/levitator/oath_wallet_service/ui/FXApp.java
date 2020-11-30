package com.levitator.oath_wallet_service.ui;

import com.levitator.oath_wallet_service.Config;
import javafx.application.Application;
import javafx.application.Platform;

import javafx.stage.Stage;
import javax.swing.JOptionPane;

// This is how we start JavaFX
public class FXApp extends Application{

    private String[] argv;
    private SystemTrayUI tray_ui;
    
    //FX demands a default constructor
    public FXApp(){}
        
    @Override
    public void start(Stage stage) throws Exception {
        
        try{
        //Don't exit just because there are no FX windows open
        Platform.setImplicitExit(false);
        
        //Put the icon in the system tray
        tray_ui = new SystemTrayUI();
        
        
        //var test = new FXMLTest();
        //test.show();
        }
        catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Unexpected error. Exiting. Error: " + ex.getLocalizedMessage(), 
                    Config.instance.app_name, JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}