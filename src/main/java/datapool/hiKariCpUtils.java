package datapool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import config.LogManager;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class hiKariCpUtils {
	// 首先定义私有的datasource
	private static DataSource datasource;
	private static HikariConfig config = null;

	// 把配置文件部分放在静态代码块中，调用时直接加载
	static {
		try {
			// 加载文件
			InputStream is = hiKariCpUtils.class.getClassLoader()
					.getResourceAsStream("hikaricp.properties");
			// 实例化properties集合
			Properties prop = new Properties();
			prop.load(is);
			// 首先加载核心类
			config = new HikariConfig(prop);
			datasource = new HikariDataSource(config);
		} catch (Exception e) {
			LogManager.log("E",e.getMessage()) ;
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

	public static void free(ResultSet rs, PreparedStatement ps, Connection conn) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			LogManager.log("E",e.getMessage()) ;
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
			} catch (SQLException e) {
				LogManager.log("E",e.getMessage()) ;
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (Exception e) {
						LogManager.log("E",e.getMessage()) ;
					}
				}
			}
		}
	}
}
