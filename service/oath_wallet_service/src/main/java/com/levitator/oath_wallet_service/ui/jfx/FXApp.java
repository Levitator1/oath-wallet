package com.levitator.oath_wallet_service.ui.jfx;

import com.levitator.oath_wallet_service.Main;
import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.ui.console.ConsolePopup;
import com.levitator.oath_wallet_service.ui.console.ConsoleWindow;
import com.levitator.oath_wallet_service.ui.SystemTrayUI;
import com.levitator.oath_wallet_service.ui.console.ConsoleController;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

// This is how we start JavaFX and the system tray icon, which is AWT
public class FXApp extends Application{
        
    static private CompletableFuture<FXApp>  m_last_app = new CompletableFuture<>();
    
    private String[] argv;
    private SystemTrayUI tray_ui;
    private ConsoleWindow console_ui;
    private ConsolePopup console_popup_ui;
    private ConsoleController console_popup_controller;
    //private Stage primary_stage;
   
    
    //FX demands a default constructor
    public FXApp(){                
    }
   
    //A stupid solution to a stupid problem
    static public FXApp last_app_started() throws InterruptedException, ExecutionException{
        var result =  m_last_app.get();
        m_last_app = new CompletableFuture<>();
        return result;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        
        m_last_app.complete(this);
        
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
            console_popup_controller = console_popup_ui.controller(ConsoleController.class);
        }
        catch(Exception ex){
            Service.fatal("Unexpected error in GUI thread. Exiting.", ex);
            Main.exit(-1);
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
        console_popup_ui.setX(p.x);
        console_popup_ui.setX(p.x);
        console_popup_ui.setY(p.y);
        console_popup_ui.setY(p.y);
        console_popup_ui.show();
    }

    private void write_console_text_impl(String text, Font font){
        var text_element = new Text(text);
        if(font != null)
            text_element.setFont(font);
        console_popup_controller.add_text(text_element);
    }
    
    public void write_console_text(String text, Font font) {
        Platform.runLater( ()->{ write_console_text_impl(text, font); } );
    }

    public void set_status_text(String text) {
        Platform.runLater( () ->{ console_popup_controller.set_status_text(text); } );
    }
}
