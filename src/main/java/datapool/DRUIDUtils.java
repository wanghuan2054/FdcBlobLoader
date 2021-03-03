package datapool;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSourceFactory;

public class DRUIDUtils {
    // 首先定义私有的datasource
    private static DataSource datasource;

    // 把配置文件部分放在静态代码块中，调用时直接加载
    static {
        try {
            // 加载文件
            InputStream is = DRUIDUtils.class.getClassLoader()
                    .getResourceAsStream("druidconfig.properties");
            // 实例化properties集合
            Properties prop = new Properties();
            prop.load(is);
            // 首先加载核心类
            datasource = DruidDataSourceFactory.createDataSource(prop);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 提供获得数据源
    public static DataSource getDataSource() {
        return datasource;
    }

    // 提供获得连接
    public static Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }
}