package config;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Properties;

import blobData.ConstantDefinition;

/**
 * 
 * @author 10024908 Class for reading properties configuration files
 */
public class ConfigManager {
	private static String configPath = ConstantDefinition.DEFAULT_CONFIGFILE;

	private ConfigManager() {
	};

	/**
	 * Use default properties file path, read the default file and return properties object pro
	 * 
	 * @param filename
	 * @return
	 */
	public static Properties readProperties() {
		Properties props = new Properties();
		InputStream in = null;
		try {
			 in = ConfigManager.class.getClassLoader().getResourceAsStream(
			 "config/" + configPath);
			// String path = LogManager.class.getClassLoader()
			// .getResource(configPath).getPath();
			// System.out.println(path);
			props.load(in);
		} catch (FileNotFoundException e) {
			LogManager.log("E", "Config Properties File not found "
					+ configPath);
		} catch (IOException e) {
			LogManager.log("E", "Config File IO Exception " + configPath);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				LogManager.log("E",
						"properties config IO inputStream close error...");
			}
		}
		return props;
	}

	/**
	 * Pass a properties file path, read the file and return properties object pro
	 * 
	 * @param filename
	 * @return
	 */
	public static Properties readProperties(String configPath) {
		Properties props = new Properties();
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(configPath));
			props.load(in);
		} catch (FileNotFoundException e) {
			LogManager.log("E", "Config Properties File not found");
		} catch (IOException e) {
			LogManager.log("E", "Config File IO Exception");
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				LogManager.log("E",
						"properties config IO inputStream close error...");
			}
		}
		return props;
	}
}
