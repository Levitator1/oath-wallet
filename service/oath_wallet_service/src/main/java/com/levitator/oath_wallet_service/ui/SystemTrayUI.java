
package com.levitator.oath_wallet_service.ui;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import com.levitator.oath_wallet_service.Config;
import com.levitator.oath_wallet_service.Main;
import com.levitator.oath_wallet_service.ui.awt.MouseListenerBase;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class SystemTrayUI {

    private SystemTray system_tray;
    private Image tray_icon_image;
    private TrayIcon tray_icon;
    
        
    public SystemTrayUI() throws Exception{
        
        if(!SystemTray.isSupported())
            throw new Exception("No system tray available");
        system_tray = SystemTray.getSystemTray();
    
        //The tray icon comes out pretty ugly, as it seems to be drawn with a white background
        //which is visible through the alpha channel, and it obscures the normal taskbar color
        //behind it, so if the icon is alhpa-stenciled, then it appears on a white square.
        //There are ways to influence the transparency of normal AWT windows, but the system tray
        //is not one of them, so I don't see how to improve this. Surely someone else has
        //accomplished this before, or maybe Java taskbar icons always look like shit.
        
        //Create the tray icon
        tray_icon_image = Config.instance.window_icon_image;
        tray_icon = new TrayIcon(tray_icon_image, "OATH Wallet for Yubikey and Firefox");
        tray_icon.setImageAutoSize(true);
                
        //Create a popup menu for the tray icon
        //Getting the tray menu to pop up for left clicks seems to be among the
        //conceptually trivial things that are nonetheless impossible in Java, or at least AWT.
        //It's always right-clicks.
        var menu = new PopupMenu();
        var exit_menu = new MenuItem(format_menu_text("Exit"));        
        exit_menu.addActionListener((ae) -> { handle_exit_click(ae); });
        menu.addSeparator(); //Bulk up the menu a bit because it looks pretty insubstantial
        menu.add(exit_menu);        
        menu.addSeparator();
        tray_icon.setPopupMenu(menu);
               
        system_tray.add(tray_icon);        
    }
    
    private String format_menu_text(String text){
        return "     " + text + "     ";
    }
    
    
    
    void handle_exit_click(ActionEvent ae){
        Main.exit(0);
    }
    
}
