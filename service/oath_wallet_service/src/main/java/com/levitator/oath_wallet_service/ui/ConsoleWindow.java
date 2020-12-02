package com.levitator.oath_wallet_service.ui;

import java.io.IOException;
import javafx.stage.StageStyle;


public class ConsoleWindow extends FXMLWindow {
    
    //jfx demands that the style be set before the window is ever displayed
    public ConsoleWindow() throws IOException {
        super("Console");
        titleProperty().set("OATH Wallet Console");        
    }
}
