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
        // Priority: Program Arguments > System.getProperty("STARTTIME") > current JVM time minus 120 minutes
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
                            + ", thread needs to suspend for 5 minutes waiting");
                    // If endTime is greater than current time, wait for 5 minutes
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    LogManager.log("E", e.getMessage());
                }
            }
            ResultSet rs;
            long t1 = System.currentTimeMillis();
            /* Used when testing multiple groups of data within specified startTime-endTime */
            rs = DBUtil.getInstance().getRecordsFromEQP_TRACE_TRX_FDC(
                    startTime, endTime);
            LogManager.log("I", "\nQuerying EQP_TRACE_TRX_FDC between " + startTime
                    + " to " + endTime);

            /* Used when testing single Glass data */
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
