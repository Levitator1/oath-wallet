package com.levitator.oath_wallet_service.ui;
import java.io.IOException;



public class ConsolePopup extends FXMLSystemTrayPopup {
    
    public ConsolePopup(double x, double y) throws IOException {
        super("Console", x, y);
    }
    
    public ConsolePopup() throws IOException {
        super("Console");
    }
}

