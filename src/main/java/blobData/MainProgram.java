package blobdata;

import Utils.DBUtil;
import Utils.DateUtils;
import config.LogManager;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author 10024908
 */
public class MainProgram {
    public static void main(String[] args) {
        // 优先取Program Arguments 参数，其次取System.getProperty("STARTTIME") Java运行设置参数，若都不存在则取当前JVM时间向前推120Mins
        String startTime = args[0] == null ? (System.getProperty("STARTTIME") == null ? DateUtils.getBeforeTime(DateUtils.getCurrentTime()) : System.getProperty("STARTTIME")) : args[0];
        String endTime = null;
        boolean flag = true;
        while (true) {
            if (flag) {
                flag = false;
            } else {
                startTime = endTime;
            }
            endTime = DateUtils.getAfterTime(startTime);
            while (endTime.compareTo(DateUtils.getCurrentTime()) >= 0) {
                try {
                    LogManager.log("I", "endTime:" + endTime + " > "
                            + "currentTime:" + DateUtils.getCurrentTime()
                            + ",需要线程挂起5分钟等待");
                    // 如果endTime 大于 当前时间，则等待5分钟
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    LogManager.log("E", e.getMessage());
                }
            }
            ResultSet rs;
            long t1 = System.currentTimeMillis();
            /* 测试指定startTime-endTime多组数据时采用 */
            rs = DBUtil.getInstance().getRecordsFromEQP_TRACE_TRX_FDC(
                    startTime, endTime);
            LogManager.log("I", "\n正在查询 EQP_TRACE_TRX_FDC between " + startTime
                    + " to " + endTime);

            /* 测试单个数据， 单条 Glass data 时采用 */
              /*rs = DBUtil.getInstance().getSingleRescordsFromEQP_TRACE_TRX_FDC(
			 "BOE/B6/LBP_2F/LTPS:6LTDH01/6LTDH01-CHMB", "6LF6990046A2",
			 "6LTDH01_DCP"); */

            FDCTraceParserBlob fdcTraceParserBlob;
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
