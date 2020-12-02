package com.levitator.oath_wallet_service.ui;

import com.levitator.oath_wallet_service.Main;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ConsoleController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button quit_button;

    @FXML
    void handle_quit_button(ActionEvent event) {
        Main.exit(0);
    }

    @FXML
    void initialize() {
        assert quit_button != null : "fx:id=\"quit_button\" was not injected: check your FXML file 'Console.fxml'.";

    }
}
