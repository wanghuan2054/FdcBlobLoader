package datapool;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import config.LogManager;

public class hiKariCpUtils {
	// First define private datasource
	private static DataSource datasource;
	private static HikariConfig config = null;

	// Put configuration file part in static block, load directly when called
	static {
		try {
			// Load file
			InputStream is = hiKariCpUtils.class.getClassLoader()
					.getResourceAsStream("datapool/hikaricp.properties");
			// Instantiate properties collection
			Properties prop = new Properties();
			prop.load(is);
			// First load core class
			config = new HikariConfig(prop);
			datasource = new HikariDataSource(config);
		} catch (Exception e) {
			LogManager.log("E",e.getMessage()) ;
		}
	}

	// Provide method to get data source
	public static DataSource getDataSource() {
		return datasource;
	}
 
	// Provide method to get connection
	public static Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	public static void free(ResultSet rs, PreparedStatement ps, Connection conn) {
		try {
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
			LogManager.log("E",e.getMessage()) ;
		} finally {
			try {
				if (ps != null)
					ps.close();
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
