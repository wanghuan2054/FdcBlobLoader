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
	 * 得到指定分钟后的时间 传入 str类型 返回 指定分钟后的str类型 input 20171201 070000 （30分钟） return
	 * 20171201 073000
	 */
	public static String getAfterTime(String str) {

		Date dBefore = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
		Calendar calendar = Calendar.getInstance(); // 得到日历
		try {
			calendar.setTime(format.parse(str)); // 把当前时间赋给日历
		} catch (ParseException e) {
			System.err.println();
			LogManager
					.log("E", "date format is error :" + str + e.getMessage());
		}
		calendar.add(Calendar.MINUTE, 30); // 设置为30 minute 后
		dBefore = calendar.getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss"); // 设置时间格式
		String defaultStartDate = sdf.format(dBefore);

		return defaultStartDate;
	}

	/**
	 * 
	 * @return 20171201 073000 str 类型
	 */
	public static String getCurrentTime() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss"); // 设置时间格式
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
							+ ",需要线程挂起5分钟等待");
					Thread.sleep(300000); // 如果endTime 大于 当前时间，则等待5分钟
				} catch (InterruptedException e) {
					LogManager.log("E", e.getMessage());
				}
			}
			ResultSet rs = null;
			long t1 = System.currentTimeMillis();
			/* 测试指定startTime-endTime多组数据时采用 */
			rs = DBUtil.getInstance().getRecordsFromEQP_TRACE_TRX_FDC(
					startTime, endTime);
			LogManager.log("I", "\n正在查询 EQP_TRACE_TRX_FDC between " + startTime
					+ " to " + endTime);

			/* 测试单个数据， 单条 Glass data 时采用 */
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
					LogManager.log("I", "第 " + count
							+ " 条Glass Blob data process finished");
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

			// 输出程序总执行时间
			LogManager.log("I", "From " + startTime + " to " + endTime + " "
					+ count + " 条数据" + " 共计耗时:" + (t2 - t1) / 1000.0 / 60.0
					+ "minutes");

			LogManager.log("I", "半小时数据查询完毕，需要线程挂起等待下一次查询");
		}

	}
}
