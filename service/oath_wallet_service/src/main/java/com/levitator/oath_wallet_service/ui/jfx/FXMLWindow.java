package com.levitator.oath_wallet_service.ui.jfx;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/*
*
* Your basic application Window, loaded from fxml
*
*/
public class FXMLWindow extends Stage{
    
    private Object m_controller;
    
    private void init(String name) throws IOException{
        var loader = new FXMLSceneLoader();
        setScene( loader.load(name) );
        m_controller = loader.controller();
    }
    
    public FXMLWindow(String name, StageStyle style) throws IOException{
        super(style);
        initStyle(style);
        init(name);
    }
    
    public FXMLWindow(String name) throws IOException {
        super(StageStyle.DECORATED);        
        init(name);
    }
    
    public <T, C extends Class<T>> T controller(C cls){     
        return cls.cast(m_controller);
    }
}
