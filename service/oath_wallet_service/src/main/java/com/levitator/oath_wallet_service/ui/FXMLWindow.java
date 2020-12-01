package com.levitator.oath_wallet_service.ui;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/*
*  A general base class for an fxml window
*  I question the benefits of FXML given that you can't step through
*  it in a debugger, and I don't yet see any syntax advantages.
*  Maybe it's easier to group lots of assignments without lots of textual redundancy.
*/
public class FXMLWindow extends Stage{
   public FXMLWindow(String name) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/levitator/oath_wallet_service/resources/fxml/" + name + ".fxml"));
        var scene = fxmlLoader.<Scene>load();                
        setScene(scene);    
   }
}

