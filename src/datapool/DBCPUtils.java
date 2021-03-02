package datapool;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;

public class DBCPUtils {

	public DBCPUtils() {
		// TODO Auto-generated constructor stub
	}

	// 首先定义私有的datasource
	private static DataSource datasource;

	// 把配置文件部分放在静态代码块中，调用时直接加载
	static {

		try {
			// 加载文件
			InputStream is = DBCPUtils.class.getClassLoader()
					.getResourceAsStream("datapool/dbcpconfig.properties");
			// 实例化properties集合
			Properties prop = new Properties();
			prop.load(is);
			// 首先加载核心类
			datasource = BasicDataSourceFactory.createDataSource(prop);
			// datasource = DruidDataSourceFactory.createDataSource(prop);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// 提供获得数据源
	public static DataSource getDataSource() {
		return datasource;
	}

	// 提供获得连接
	public static Connection getMDWConnection() throws SQLException {
		return datasource.getConnection();
	}
}
