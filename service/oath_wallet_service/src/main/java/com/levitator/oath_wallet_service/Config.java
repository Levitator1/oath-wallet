package com.levitator.oath_wallet_service;

import java.io.File;
import java.nio.file.Path;

/*
*/
public class Config {

    //Suitable for a Linux-style environment. Windows users will want something else.
    //TODO: Support Windows
    static final Path config_dir = Path.of("~/.oath_wallet/"); 
    static final String domain_config_name = "mappings.json";
    static final File domain_config = config_dir.resolve(domain_config_name).toFile();    
}
