package com.levitator.oath_wallet_service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/*
*/
public class Config {

    public final String app_name = "oath-wallet";
    
    //Suitable for a Linux-style environment. Windows users will want something else.
    //TODO: Support Windows
    public final Path config_dir = Path.of("~/.oath_wallet/");
    
    public final String domain_config_name = "mappings.json";
    public final File domain_config = config_dir.resolve(domain_config_name).toFile();
    public final Image fallback_image = draw_fallback_image();
    public final Image window_icon_image = get_image("/com/levitator/oath_wallet_service/resources/icons/tray_icon.png");
    public final Icon window_icon = new ImageIcon(window_icon_image);
    static public final Config instance = new Config();
    
    private static Image draw_fallback_image(){
        var img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.MAGENTA);
        g2.fill3DRect(0, 0, 50, 50, false);
        return img;
    }
    
    //Retrieve an icon by pathname, or if it is not found, then return a generated placeholder image
    private Image get_image( String path ){        
        try{            
            return ImageIO.read( Config.class.getResource(path) );
        }
        catch( IOException ex ){
           return fallback_image;
        }        
    }        
}
