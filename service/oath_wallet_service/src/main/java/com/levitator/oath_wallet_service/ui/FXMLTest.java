package com.levitator.oath_wallet_service.ui;

import java.io.IOException;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FXMLTest extends Stage {
    @FXML private TextField textField;

    public FXMLTest() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/levitator/oath_wallet_service/resources/fxml/test.fxml"));        
        VBox vbox = fxmlLoader.<VBox>load();
        
        Scene scene = new Scene(vbox);
        setScene(scene);
                
        //this.getScene().setRoot(vbox);
        //fxmlLoader.setRoot(this);
        //fxmlLoader.setController(this);        
    }

    public String getText() {
        return textProperty().get();
    }

    public void setText(String value) {
        textProperty().set(value);
    }

    public StringProperty textProperty() {
        return textField.textProperty();
    }

    @FXML
    protected void doSomething() {
        System.out.println("The button was clicked!");
    }
}
