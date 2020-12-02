package com.levitator.oath_wallet_service.ui.jfx;

import com.levitator.oath_wallet_service.Service;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Window;

/*
*
* Tools for coping with JFX
*
*/
public class JFX {
    
    //Clamp a Window's coordinates so that it sits inside a specified margin around its containing screen
    static public void enforce_screen_x_margin(Window win, double margin){
        
        var x = win.getX();
        var y = win.getY();
                
        //Enforce a margin inside the edge of the screen so that the window doesn't get concealed by taskbars
        var screens = Screen.getScreensForRectangle(x, y, 0, 0);        
        
        //I guess we could get more than one screen. Might as well just pick the first one.
        if(screens.size() > 0){            
            var dimensions = screens.get(0).getBounds();
            
            win.setX( Math.max(x, dimensions.getMinX() + margin));
            win.setX( Math.min(x, dimensions.getMaxX() - margin - win.getWidth()));
        }
    }
    
    //Clamp a Window's coordinates so that it sits inside a specified margin around its containing screen
    static public void enforce_screen_y_margin(Window win, double margin){
        
        var x = win.getX();
        var y = win.getY();
                
        //Enforce a margin inside the edge of the screen so that the window doesn't get concealed by taskbars
        var screens = Screen.getScreensForRectangle(x, y, 0, 0);        
        
        //I guess we could get more than one screen. Might as well just pick the first one.
        if(screens.size() > 0){            
            var dimensions = screens.get(0).getBounds();            
            win.setY( Math.max(y, dimensions.getMinY() + margin));
            win.setY( Math.min(y, dimensions.getMaxY() - margin - win.getHeight()));
        }
    }
    
}
