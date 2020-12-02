package com.levitator.oath_wallet_service.ui;

import com.levitator.oath_wallet_service.Service;
import com.levitator.oath_wallet_service.ui.jfx.JFX;
import java.io.IOException;
import javafx.event.EventType;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/*
*
* Intended for system tray popup windows
*
*/
public class FXMLSystemTrayPopup extends FXMLWindow{
    
    private boolean x_adjust_reentry = false, y_adjust_reentry = false;
    
    
    private void init(String name) throws IOException{
        //var scene = FXMLSceneLoader.load(name);
        //scene.setRoot(scene.getRoot());
        setScene( FXMLSceneLoader.load(name));
        initStyle(StageStyle.UNDECORATED);
        
        //The taskbar widget which is not supposed to show up in the first place
        //nonetheless obnoxiously permits maximization, so remember to trap the
        //maximization event and undock the window
        setResizable(false);               
        
        //This is not supposed to happen either, but since it does, we have to deal with it
        iconifiedProperty().addListener( (o) -> { if (isIconified()) hide(); }  );
        showingProperty().addListener( (o) -> { setIconified(false); });
     
        //Because there is no reasonable native way to do a system tray popup, we pop up a window in the general
        //vacinity of the mouse at the point in time at which we receive a system tray click, so we need to nudge
        //that position inward so that the window fits inside the screen, being that system trays are usually way out
        //at the edge of the screen
        
        //Enforce a margin inside the edge of the screen so that the window doesn't get concealed by taskbars
        var margin = 32;
        xProperty().addListener( (o) ->{
            if(x_adjust_reentry) return;
            x_adjust_reentry = true;
            try{
                JFX.enforce_screen_x_margin(this, margin);
            }
            finally{ x_adjust_reentry = false; }
        } );
        
        yProperty().addListener( (o) ->{
            if(y_adjust_reentry) return;
            y_adjust_reentry = true;
            try{
                JFX.enforce_screen_y_margin(this, margin);
            }
            finally{ y_adjust_reentry = false; }
        });
        
        focusedProperty().addListener( (o) -> {
            if(!isFocused())
                hide();
        });
        
        //This does nothing to prevent maximization from the taskbar widget that shouldn't exist
        setMaxHeight(getHeight());
        setMaxWidth(getWidth());
        
        
        //Brute-force to defeat the evil taskbar widget
        maximizedProperty().addListener( (o) -> {
            setMaximized(false);
        });                        
    }
    
    public FXMLSystemTrayPopup( String name, double x, double y) throws IOException{
        super(name, StageStyle.UNDECORATED);
        setX(x);
        setY(y);
        init(name);
    }
    
    public FXMLSystemTrayPopup( String name ) throws IOException{
        super(name, StageStyle.UNDECORATED);
        init(name);
    }            
}