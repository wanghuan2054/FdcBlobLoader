package config;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import blobdata.ConstantDefinition;

/**
 * @author 10024908 读取 properties 配置文件的类
 */
public class ConfigManager {
    private static String configPath = ConstantDefinition.DEFAULT_CONFIGFILE;

    private ConfigManager() {
    }

    ;

    /**
     * 启用默认properties文件路径 ， 读取该默认文件并返回properties对象pro
     *
     * @param
     * @return
     */
    public static Properties readProperties() {
        Properties props = new Properties();
        InputStream in = null;
        try {
			 in = ConfigManager.class.getClassLoader().getResourceAsStream(
			  configPath);

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
     * 传入一个properties文件路径 ， 读取该文件并返回properties对象pro
     *
     * @param
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
