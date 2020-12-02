package com.levitator.oath_wallet_service.ui.jfx;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

/*
* Just a common way of loading scenes by name while specifying the path
* resolution in one common place.
*/
public class FXMLSceneLoader{
    
    private Object m_controller;
    
    public Scene load(String name) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(FXMLSceneLoader.class.getResource("/com/levitator/oath_wallet_service/resources/fxml/" + name + ".fxml"));
        var scene = fxmlLoader.<Scene>load();
        m_controller = fxmlLoader.getController();
        return scene;
    }         

    Object controller() {
        return m_controller;
    }
}
