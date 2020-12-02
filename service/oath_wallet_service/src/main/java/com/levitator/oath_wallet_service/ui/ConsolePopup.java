package com.levitator.oath_wallet_service.ui;
import java.io.IOException;



public class ConsolePopup extends FXMLPopup {
    
    public ConsolePopup(double x, double y) throws IOException {
        super("Console", x, y);
    }
    
    //jfx demands that the style be set before the window is ever displayed
    public ConsolePopup() throws IOException {
        super("Console");                
    }
}