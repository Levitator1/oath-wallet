package com.levitator.oath_wallet_service.ui;

import java.io.IOException;
import javafx.stage.Popup;

/*
*
* An undecorated window for context menus and such
*
*/
public class FXMLPopup extends Popup{
    
    private void init(String name) throws IOException{
        var scene = FXMLSceneLoader.load(name);
        scene.setRoot(scene.getRoot());
        //setScene( FXMLSceneLoader.load(name));
        //initStyle(StageStyle.UNDECORATED);
    }
    
    public FXMLPopup( String name, double x, double y) throws IOException{
        //super(name, StageStyle.UNDECORATED);
        setX(x);
        setY(y);
        init(name);
    }
    
    public FXMLPopup( String name ) throws IOException{
        //super(name, StageStyle.UNDECORATED);
        init(name);
    }
}
