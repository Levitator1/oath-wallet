package com.levitator.oath_wallet_service.ui;

import com.levitator.oath_wallet_service.Config;
import com.levitator.oath_wallet_service.util.Util;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
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
    //private Stage primary_stage;
   
    
    //FX demands a default constructor
    public FXApp(){}
        
    @Override
    public void start(Stage stage) throws Exception {
        
        try{
            //Don't exit just because there are no FX windows open
            Platform.setImplicitExit(false);                       
              
            //This stuff is part of some recipe I found online for tricking the
            //Java runtime into displaying a window without a taskbar widget.
            //It involves parenting the popup window to a hidden primary window/stage
            //It doesn't work under OpenJDK.
            /*
            primary_stage = stage;
            stage.initStyle(StageStyle.UTILITY);
            stage.setWidth(0);
            stage.setHeight(0);
            stage.setOpacity(0d);
            */
            
            //Put the icon in the system tray
            tray_ui = new SystemTrayUI( (me) -> {
                Platform.runLater( () -> { handle_tray_click_message(me); } );
            });
            
            //Create UI windows         
            console_ui = new ConsoleWindow();
            console_popup_ui = new ConsolePopup();                       
        }
        catch(Exception ex){
            JOptionPane.showMessageDialog(null, "Unexpected error. Exiting. Error: " + Util.full_stack_trace_string(ex),
                    Config.instance.app_name, JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }    
              
    //Ok, so I wasted inordinate hours of my life pursuing the seemingly trivial
    //goal of providing a popup window which does not show up in the taskbar only
    //to discover that it has been in the Open JDK bug queue for nine years.
    //So, it shows up in the taskbar. Maybe it won't under some other runtime.
    private void handle_tray_click_message(MouseEvent me){
        
        if(console_popup_ui.isShowing()){
            console_popup_ui.hide();
            return;
        }
        
        Point p = MouseInfo.getPointerInfo().getLocation();        
        
        //The screen margin doesn't get applied properly the first time. Don't know why.
        console_popup_ui.show(); 
        console_popup_ui.setX(p.x);
        console_popup_ui.setX(p.x);
        console_popup_ui.setY(p.y);
        console_popup_ui.setY(p.y);                           
    }
}
