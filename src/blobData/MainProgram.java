package blobData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import config.LogManager;

public class MainProgram {
	/*
	 * Get time after specified minutes
	 * Input str type, return str type after specified minutes
	 * input 20171201 070000 (30 minutes) return 20171201 073000
	 */
	public static String getAfterTime(String str) {

		Date dBefore = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
		Calendar calendar = Calendar.getInstance(); // Get calendar
		try {
			calendar.setTime(format.parse(str)); // Assign current time to calendar
		} catch (ParseException e) {
			System.err.println();
			LogManager
					.log("E", "date format is error :" + str + e.getMessage());
		}
		calendar.add(Calendar.MINUTE, 30); // Set to 30 minutes later
		dBefore = calendar.getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss"); // Set time format
		String defaultStartDate = sdf.format(dBefore);

		return defaultStartDate;
	}

	/**
	 * 
	 * @return 20171201 073000 str type
	 */
	public static String getCurrentTime() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss"); // Set time format
		String currentDate = sdf.format(new Date());

		return currentDate;
	}

	public static void main(String[] args) {

//		 String startTime = "20190724 060000";
		 String startTime = "20210201 060000";
//		String startTime = System.getProperty("STARTTIME");
		String endTime = null;
		boolean flag = true;
		while (true) {
			if (flag) {
				flag = false;
			} else {
				startTime = endTime;
			}
			endTime = getAfterTime(startTime);
			while (endTime.compareTo(getCurrentTime()) >= 0) {
				try {
					LogManager.log("I", "endTime:" + endTime + " > "
							+ "currentTime:" + getCurrentTime()
							+ ", thread needs to suspend for 5 minutes waiting");
					Thread.sleep(300000); // If endTime is greater than current time, wait for 5 minutes
				} catch (InterruptedException e) {
					LogManager.log("E", e.getMessage());
				}
			}
			ResultSet rs = null;
			long t1 = System.currentTimeMillis();
			/* Used when testing multiple groups of data within specified startTime-endTime */
			rs = DBUtil.getInstance().getRecordsFromEQP_TRACE_TRX_FDC(
					startTime, endTime);
			LogManager.log("I", "\nQuerying EQP_TRACE_TRX_FDC between " + startTime
					+ " to " + endTime);

			/* Used when testing single Glass data */
/*			 rs = DBUtil.getInstance().getSingleRescordsFromEQP_TRACE_TRX_FDC(
			 "BOE/B6/LBP_2F/LTPS:6LTDH01/6LTDH01-CHMB", "6LF6990046A2",
			 "6LTDH01_DCP"); // 6LWN6Y0731C3
*/
			FDCTraceParserBlob fdcTraceParserBlob = null;
			int count = 0;
			try {
				while (rs.next()) {
					count++;
					fdcTraceParserBlob = new FDCTraceParserBlob();

					fdcTraceParserBlob.groupByStepAndTime(fdcTraceParserBlob
							.createTable(rs));
					LogManager.log("I", "The " + count
							+ "th Glass Blob data process finished");
				}
			} catch (SQLException e) {
				LogManager.log("E", e.toString());
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (SQLException e) {
						LogManager.log("E", e.toString() + " rs close error");
					}
				}
			}

			long t2 = System.currentTimeMillis();

			// Output total program execution time
			LogManager.log("I", "From " + startTime + " to " + endTime + " "
					+ count + " records, total time elapsed: " + (t2 - t1) / 1000.0 / 60.0
					+ "minutes");

			LogManager.log("I", "Half-hour data query completed, thread needs to suspend waiting for next query");
		}

	}
}
