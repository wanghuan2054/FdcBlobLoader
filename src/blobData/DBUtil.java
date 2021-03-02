package blobData;

import java.io.FileOutputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;

import oracle.sql.BLOB;
import config.ConfigManager;
import config.LogManager;
import datapool.hiKariCpUtils;

public class DBUtil {
	private static DBUtil dbUtils = null;
	private boolean flag = false;
	private static Properties pro = new Properties();
	private static int nums = 0;

	private DBUtil() {
	}

	static {
		pro = ConfigManager.readProperties();
		String dbDriver = pro.getProperty("DBDriver");
		try {
			Class.forName(dbDriver);
		} catch (ClassNotFoundException e) {
			LogManager.log("E", "oracle driver load fail" + e.toString());
		}
	}

	public static DBUtil getInstance() {
		if (dbUtils == null) {
			synchronized (DBUtil.class) {
				if (dbUtils == null) {
					dbUtils = new DBUtil();
				}
			}
		}
		return dbUtils;
	}

	public Connection getFDCConnection() {
		String username = pro.getProperty("USERNAME");
		String password = pro.getProperty("PASSWORD");
		String url = pro.getProperty("URL");
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			LogManager.log("E", "database link error" + e.toString());
		}
		return conn;
	}

	// 使用连接池获得的 Connection
	public Connection getMDWConnection() throws SQLException {

		Connection conn = null;
		try {
			// 使用 DBCP 方式管理的连接池
			// conn = DBCPUtils.getMDWConnection();
			// 使用 alibaba druid 方式管理的连接池
			// conn = DRUIDUtils.getConnection();

			// 使用 HiKariCp 方式管理的连接池
			conn = hiKariCpUtils.getConnection();
		} catch (SQLException e) {
			throw new SQLException("mdw database link error " + e.toString()); // ;
		}
		return conn;
	}

	// public Connection getMDWConnection() {
	// String username = pro.getProperty("MDWUSERNAME");
	// String password = pro.getProperty("MDWPASSWORD");
	// String url = pro.getProperty("MDWURL");
	// Connection conn = null;
	// try {
	// conn = DriverManager.getConnection(url, username, password);
	// } catch (SQLException e) {
	// System.err.println("mdw database link error " + e.toString());
	// }
	// return conn;
	// }

	public boolean writeToMDW(
			LinkedHashMap<Vector<String>, List<AggregateFunction>> result) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		AggregateFunction aggregateFunction = null;
		List<AggregateFunction> paramValues = null;
		Iterator<Entry<Vector<String>, List<AggregateFunction>>> iter = result
				.entrySet().iterator();
		boolean flag = false;
		Vector<String> paramKey = null;
		try {
			conn = getMDWConnection();
		} catch (SQLException e) {
			LogManager.log("E", " Get MDW Connection Failed  ");
			return flag;
		}
		try {
			while (iter.hasNext()) {
				String sql = "INSERT INTO EDS_FDC_TRACE(START_TIMEKEY,END_TIMEKEY,SHIFT_TIMEKEY, LOT_ID, GLASS_ID, SLOT, RECIPE, PRODUCT_ID, STEP_ID,STEP_NAME,UNIT_ID,PTODUCTIONTYPE,OPER_ID,ITEM,VALUE_MIN,VALUE_MAX,VALUE_AVG) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

				conn.setAutoCommit(false);
				ps = conn.prepareStatement(sql);

				Map.Entry entry = (Map.Entry) iter.next();
				paramKey = (Vector<String>) entry.getKey();
				paramValues = (List<AggregateFunction>) entry.getValue();

				aggregateFunction = new AggregateFunction();

				for (int i = 0; i < paramValues.size(); i++) {
					nums++; // 记录每一条数据个数
					for (int j = 0; j < paramKey.size(); j++) {
						if (j == 0 || j == 1) // 0 1 时间的开始和结束两个DTTS ，需要转换成
												// TimeStamp
							/*ps.setTimestamp(j + 1,
									Timestamp.valueOf(paramKey.get(j)));*/
							ps.setString(j + 1,
									 DateUtils.timeStampToString(Timestamp.valueOf(paramKey.get(j)))
									 /*String.valueOf(paramKey.get(j))*/);
						else
							ps.setString(j + 1, paramKey.get(j));
					}
					aggregateFunction = paramValues.get(i);
					ps.setString(14, aggregateFunction.getItemName());
					ps.setDouble(15, aggregateFunction.getMinValue());
					ps.setDouble(16, aggregateFunction.getMaxValue());
					ps.setDouble(17, aggregateFunction.getAvgValue());
					ps.addBatch();
					// // 每5000条，提交一次;这里不能一次提交过多的数据 。
					// if (nums > 500) {
					// ps.executeBatch();
					// conn.commit();
					// nums = 0;
					// }
					// ps.executeUpdate();
				}
				ps.executeBatch();
				conn.commit();
			}
		} catch (SQLException e1) {
			try {
				// 若出现异常，对数据库中所有已完成的操作全部撤销，则回滚到事务开始状态
				if (!conn.isClosed()) {
					conn.rollback();// 4,当异常发生执行catch中SQLException时，记得要rollback(回滚)；
					LogManager.log("I", " 插入失败，回滚！" + e1.getMessage().trim()
							+ paramKey.toString()
							+ ConstantDefinition.DELEMETER_SPACE
							+ aggregateFunction.getItemName());
					conn.setAutoCommit(true);
				}
			} catch (SQLException e2) {
				LogManager.log("E", e2.toString());
			}
		} finally {
			dbResourceFree(rs, ps, conn);
		}
		flag = true;
		return flag;
	}

	/*
	 * Fuction description : 从 EQP_TRACE_TRX_FDC 拿数据 input :传入 startTime -
	 * endTime（一般为一天 按天为单位） return : 返回这个时间段内的符合package中筛选条件的Glass 记录集
	 */
	public ResultSet getSingleRescordsFromEQP_TRACE_TRX_FDC(String moduleId,
			String glassID, String dcpID) {
		Connection conn = getFDCConnection();
		ResultSet rs = null;
		BLOB blob = null;
		PreparedStatement cstmt = null;
		String sql = "select * from EQP_TRACE_TRX_FDC where EQP_MODULE_ID = ? and SUBSTRATE_ID = ? and EQP_DCP_ID = ?";
		try {
			cstmt = conn.prepareCall(sql);
			cstmt.setString(1, moduleId);
			cstmt.setString(2, glassID);
			cstmt.setString(3, dcpID);

			cstmt.executeQuery();
			rs = cstmt.getResultSet();
		} catch (SQLException e) {
			LogManager.log("E", e.toString());
		} finally {
			// dbResourceFree(cstmt, conn);
		}
		return rs;
	}

	/*
	 * Fuction description : 从 EQP_TRACE_TRX_FDC 拿数据 input :传入 startTime -
	 * endTime（一般为一天 按天为单位） return : 返回这个时间段内的符合package中筛选条件的Glass 记录集
	 */
	public ResultSet getRecordsFromEQP_TRACE_TRX_FDC(String startTime,
			String endTime) {
		Connection conn = getFDCConnection();
		ResultSet rs = null;
		BLOB blob = null;
		CallableStatement cstmt = null;
		try {
			cstmt = conn
//					.prepareCall("{? = call PKG_FDC_TRACE_TRX.GET_FILEDATA(?,?)}");
					.prepareCall("{? = call PKG_FDC_TRACE_TRX.GET_DRYPUMPDATA(?,?)}");
			cstmt.setString(2, startTime);
			cstmt.setString(3, endTime);

			cstmt.registerOutParameter(1, oracle.jdbc.OracleTypes.CURSOR);
			cstmt.execute();
			rs = (ResultSet) cstmt.getObject(1);
		} catch (SQLException e) {
			LogManager.log("E", e.toString());
		} finally {
			// dbResourceFree(cstmt, conn);
		}
		return rs;
	}

	/*
	 * 若解压出来的blob文件中 缺少moduleName、ProductiType、stepID 会从getBlob的时候同时拿到
	 * moduleName 、ProductiType、 StepID三个参数 补充Blob中缺失的参数 返回封装这三个参数的对象 RSDData
	 */
	public RSDData parserBlobData(ResultSet rs) {
		Connection conn = getFDCConnection();
		PreparedStatement ps = null;
		BLOB blob = null;
		FileOutputStream out = null;
		RSDData rsdData = new RSDData();
		String moduleName = null;
		String productionType = null;
		String operationID = null;
		try {
			if (rs != null) {

				moduleName = rs.getString(ConstantDefinition.EQP_MODULE_ID);
				moduleName = moduleName.split(ConstantDefinition.LEFT_SLASH)[moduleName
						.split(ConstantDefinition.LEFT_SLASH).length - 1];
				productionType = rs.getString(EDS_FDC_TRACEFIELD.RSD_05
						.getName());
				operationID = rs.getString(ConstantDefinition.OPERATION_ID);
				if (operationID == null) {
					operationID = ConstantDefinition.EMPTYSTRING;
				}

				rsdData.setModuleName(moduleName);
				rsdData.setProductionType(productionType);
				rsdData.setOperationName(operationID);
				blob = (BLOB) rs.getBlob(ConstantDefinition.FILE_DATA);
				FileUtils.decompressionGZip(blob, ConstantDefinition.TEMPFILE);
			} else {
				LogManager
						.log("E",
								" Data query result return empty,your SQL maybe has problem.");
			}
		} catch (Exception e) {
			LogManager.log("E", e.getMessage());
		} finally {
			dbResourceFree(ps, conn);
		}
		return rsdData;
	}

	public void dbResourceFree(ResultSet rs, PreparedStatement ps,
			Connection conn) {
		try {
			if (rs != null)
				rs.close();
		} catch (SQLException e) {
			LogManager.log("E", "rs close error");
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				LogManager.log("E", "ps close error");
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (Exception e) {
						LogManager.log("E",
								"DB connection close error " + e.getMessage());
					}
				}
			}
		}
	}

	public void dbResourceFree(CallableStatement csmt, Connection conn) {
		try {
			if (csmt != null)
				csmt.close();
		} catch (Exception e2) {
			LogManager.log("E", "csmt close error " + e2.toString());
		} finally {
			if (conn != null)
				try {
					conn.close();
				} catch (Exception e) {
					LogManager.log("E",
							"DB connection close error " + e.toString());
				}
		}
	}

	public void dbResourceFree(PreparedStatement ps, Connection conn) {
		try {
			if (ps != null)
				ps.close();
		} catch (Exception e2) {
			LogManager.log("E", "ps close error " + e2.toString());
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (Exception e) {
				LogManager
						.log("E", "DB connection close error " + e.toString());
			}
		}
	}
}
