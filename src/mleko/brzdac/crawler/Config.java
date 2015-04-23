/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mleko.brzdac.crawler;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Ini;

/**
 *
 * @author danielkrol
 */
public class Config {

	private static String configFileName = "./config.ini";
	private static Ini config = null;

	public static synchronized Ini getInstance() {
		if (config == null) {
			try {
				config = new Ini(new File(configFileName));
			} catch (IOException ex) {
				Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		return config;
	}

	public static synchronized void setConfigurationFile(String path) {
		if (config != null) {
			config = null;
		}
		configFileName = path;
	}
}
