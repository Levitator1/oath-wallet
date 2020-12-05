package com.levitator.oath_wallet_service.ui.console;

import com.levitator.oath_wallet_service.Config;
import com.levitator.oath_wallet_service.Main;
import com.levitator.oath_wallet_service.Service;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ConsoleController {
    
    @FXML
    private Scene scene;
    
    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button quit_button;
    
    @FXML
    private TextFlow text_area;
    
    @FXML
    private ScrollPane scroll_pane;
    
    @FXML
    private Label status_bar_text;

    //private Text[] text_elements;
    
    @FXML
    void handle_quit_button(ActionEvent event) {
        scene.getWindow().hide(); //Stalls shutdown to have windows visible
        Service.instance.exit(0);
    }

    @FXML
    void initialize() {        
        var path = Config.instance.fxml_dir + "Console.css";        
        scene.getStylesheets().add(path);
    }

    public void add_text(Text text) {
        var messages = text_area.getChildren();
        if(messages.size() >= Config.instance.console_buffer_length)
            messages.remove(0, 1);
            
        text_area.getChildren().add(text);
        scroll_pane.setVvalue( scroll_pane.getVmax() );
    }
    
    public void set_status_text(String text){
        status_bar_text.setText(text);
    }    
}
