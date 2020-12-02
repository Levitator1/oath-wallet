package com.levitator.oath_wallet_service.ui;

import com.levitator.oath_wallet_service.Config;
import com.levitator.oath_wallet_service.mt.MTMessage;
import com.levitator.oath_wallet_service.util.Util;
import com.sun.javafx.stage.StageHelper;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.concurrent.LinkedTransferQueue;
import javafx.application.Application;
import javafx.application.Platform;

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.swing.JOptionPane;

// This is how we start JavaFX
public class FXApp extends Application{

    private String[] argv;
    private SystemTrayUI tray_ui;
    private ConsoleWindow console_ui;
    private ConsolePopup console_popup_ui;
    private Stage primary_stage;
   
    
    //FX demands a default constructor
    public FXApp(){}
        
    @Override
    public void start(Stage stage) throws Exception {
        
        try{
            //Don't exit just because there are no FX windows open
            Platform.setImplicitExit(false);                       
   
            primary_stage = stage;
            stage.initStyle(StageStyle.UTILITY);
            stage.setWidth(0);
            stage.setHeight(0);
            stage.setOpacity(0d);
   
            
            //Put the icon in the system tray
            tray_ui = new SystemTrayUI( (me) -> {
                Platform.runLater( () -> { handle_tray_click_message(me); } );
            });
            
            //Create UI windows         
            console_ui = new ConsoleWindow();
            console_popup_ui = new ConsolePopup();
            //console_popup_ui.initOwner(stage);
            
            /*
            //Event loop
            while(true){
                var msg = message_queue.take();
                msg.run();
            }
            */
        }
        catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Unexpected error. Exiting. Error: " + Util.full_stack_trace_string(ex),
                    Config.instance.app_name, JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }    
              
    private void handle_tray_click_message(MouseEvent me){
        Point p = MouseInfo.getPointerInfo().getLocation();
        //primary_stage.show();
        console_popup_ui.setX(p.x);
        console_popup_ui.setY(p.y);
        //primary_stage.show();
        console_popup_ui.show(null);
    }
}
