package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import blobData.ConstantDefinition;

public class LogManager {
	private static Logger logger = null;
	private static String logConfigPath = ConstantDefinition.DEFAULT_LOGCONFIGFILE;
	static {
		logger = Logger.getLogger(LogManager.class.getName());
		InputStream in = LogManager.class.getClassLoader().getResourceAsStream(
				logConfigPath);
		// String path = LogManager.class.getClassLoader()
		// .getResource(logConfigPath).getPath();
		// InputStream in = null;
		try {
			// in = new FileInputStream(logConfigPath);
			Properties prop = new Properties();
			prop.load(in);
			PropertyConfigurator.configure(prop);
		} catch (IOException e) {
			LogManager.log("E", e.getMessage());
		}

	}

	public static void log(String logLevel, String logDetail) {
		if (logLevel.equals("E")) // E = error
			logger.error(logDetail);
		else if (logLevel.equals("I")) { // I = info
			logger.info(logDetail);
		} else {
			logger.info(logDetail);
		}
	}

}
