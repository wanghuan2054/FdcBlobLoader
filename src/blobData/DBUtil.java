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

	// Get Connection using connection pool
	public Connection getMDWConnection() throws SQLException {

		Connection conn = null;
		try {
			// Use DBCP managed connection pool
			// conn = DBCPUtils.getMDWConnection();
			// Use alibaba druid managed connection pool
			// conn = DRUIDUtils.getConnection();

			// Use HiKariCp managed connection pool
			conn = hiKariCpUtils.getConnection();
		} catch (SQLException e) {
			throw new SQLException("mdw database link error " + e.toString());
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

				Entry entry = (Entry) iter.next();
				paramKey = (Vector<String>) entry.getKey();
				paramValues = (List<AggregateFunction>) entry.getValue();

				aggregateFunction = new AggregateFunction();

				for (int i = 0; i < paramValues.size(); i++) {
					// Record count of each data
					nums++;
					for (int j = 0; j < paramKey.size(); j++) {
						// 0 1: start and end DTTS times, need to convert to TimeStamp
						if (j == 0 || j == 1) {
							ps.setString(j + 1,
									DateUtils.timeStampToString(Timestamp.valueOf(paramKey.get(j))));
						} else {
							ps.setString(j + 1, paramKey.get(j));
						}
					}
					aggregateFunction = paramValues.get(i);
					ps.setString(14, aggregateFunction.getItemName());
					ps.setDouble(15, aggregateFunction.getMinValue());
					ps.setDouble(16, aggregateFunction.getMaxValue());
					ps.setDouble(17, aggregateFunction.getAvgValue());
					ps.addBatch();
					// // Commit every 5000 records; cannot commit too many at once
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
				// If exception occurs, rollback all completed operations to transaction start state
				if (!conn.isClosed()) {
					// When SQLException occurs in catch block, remember to rollback
					conn.rollback();
					LogManager.log("I", " Insert failed, rollback! " + e1.getMessage().trim()
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

	/**
	 * Function description: Fetch data from EQP_TRACE_TRX_FDC
	 * input: startTime - endTime (typically one day)
	 * return: ResultSet of Glass records matching filter conditions within this time period
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

	/**
	 * Function description: Fetch data from EQP_TRACE_TRX_FDC
	 * input: startTime - endTime (typically one day)
	 * return: ResultSet of Glass records matching filter conditions within this time period
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

	/**
	 * If decompressed blob file is missing moduleName, ProductiType, stepID
	 * Will get moduleName, ProductiType, StepID from getBlob simultaneously
	 * Supplement missing parameters in Blob
	 * Return RSDData object encapsulating these three parameters
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
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			LogManager.log("E", "rs close error");
		} finally {
			try {
				if (ps != null) {
					ps.close();
				}
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
			if (csmt != null) {
				csmt.close();
			}
		} catch (Exception e2) {
			LogManager.log("E", "csmt close error " + e2.toString());
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					LogManager.log("E",
							"DB connection close error " + e.toString());
				}
			}
		}
	}

	public void dbResourceFree(PreparedStatement ps, Connection conn) {
		try {
			if (ps != null) {
				ps.close();
			}
		} catch (Exception e2) {
			LogManager.log("E", "ps close error " + e2.toString());
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (Exception e) {
				LogManager
						.log("E", "DB connection close error " + e.toString());
			}
		}
	}
}
