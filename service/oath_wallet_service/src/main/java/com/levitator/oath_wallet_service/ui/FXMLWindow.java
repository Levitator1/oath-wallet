package com.levitator.oath_wallet_service.ui;
import java.io.IOException;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/*
*
* Your basic application Window, loaded from fxml
*
*/
public class FXMLWindow extends Stage{
    
    public FXMLWindow(String name, StageStyle style) throws IOException{
        super(style);
        initStyle(style);
        setScene( FXMLSceneLoader.load(name) );
    }
    
    public FXMLWindow(String name) throws IOException {
        super(StageStyle.DECORATED);        
        setScene( FXMLSceneLoader.load(name) );
    }        
}
