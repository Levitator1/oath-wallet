
package com.levitator.oath_wallet_service.ui;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SystemTrayUI {

    private SystemTray system_tray;
    private BufferedImage tray_icon_image;
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
        tray_icon_image = ImageIO.read( getClass().getResource("/com/levitator/oath_wallet_service/resources/icons/tray_icon.png") );
        tray_icon = new TrayIcon(tray_icon_image, "OATH Wallet for Yubikey and Firefox");
        tray_icon.setImageAutoSize(true);        
        system_tray.add(tray_icon);
    }
}
