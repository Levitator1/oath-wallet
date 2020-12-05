package com.levitator.oath_wallet_service;

import com.levitator.oath_wallet_service.util.CrossPlatform;
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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/*
*/
public class Config {

    public final String pwd = System.getProperty("user.dir");
    public final String app_name = "oath-wallet";
    public final String app_version = "v0.1";
    public final String app_title = app_name + " " + app_version;    
        
    //Suitable for a Linux-style environment. Windows users will want something else.
    //TODO: Support Windows
    public final Path config_dir = CrossPlatform.get_os_user_config_dir().resolve(".oath_wallet");
    public final File service_lock_path = config_dir.resolve("service_lockfile").toFile();
    public final File client_lock_path = config_dir.resolve("client_lockfile").toFile();
    
    public final Path ykman_path = Path.of(CrossPlatform.auto_detect_command_path("ykman"));
    public final Path fifo_in_path = config_dir.resolve("fifo_in");     //Named from the service's perspective
    public final Path fifo_out_path = config_dir.resolve("fifo_out");
    public final String package_dir = "/com/levitator/oath_wallet_service/";
    public final String resource_dir = package_dir + "resources/";
    public final String fxml_dir = resource_dir + "fxml/";
    public final int console_buffer_length = 1024;    
    
    public final String domain_config_name = "mappings.json";
    public final File domain_config = config_dir.resolve(domain_config_name).toFile();
    public final Image fallback_image = draw_fallback_image();
    public final Image window_icon_image = get_image("/com/levitator/oath_wallet_service/resources/icons/tray_icon.png");
    public final Icon window_icon = new ImageIcon(window_icon_image);
    public final Font console_default_font = new Font("Monospaced", 12d);
    public final Font console_bold_font = Font.font("Monospaced", FontWeight.EXTRA_BOLD, 12.0);
    
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
