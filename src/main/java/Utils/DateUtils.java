package Utils;

import config.LogManager;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
	
	/**
	 * 
	     * timeStampToString:(timestamp转换为string timekey). 
	     * String.
	     * @author 10024908 
	     * @return 转换完成的timekey
	 */
	public static String timeStampToString(Timestamp ts) {
		
        String tsStr = "";  
        DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");  
        try {  
            tsStr = sdf.format(ts);  
            return tsStr;   
        } catch (Exception e) {  
            LogManager.log("E", e.getMessage());
        }
       return null ; 
	}

    /**
     * 得到指定分钟后的时间 传入 str类型 返回 指定分钟后的str类型 input 20171201 070000 （30分钟） return
     * 20171201 073000
     */
    public static String getAfterTime(String str) {
//      向后计算EndTime时间，默认延后30分钟
        int mins = 30 ;
        return getAfterTimeByMins(str,mins);
    }
    /**
     * 得到指定分钟后的时间 传入 str类型 返回 指定分钟前的str类型 input 20171201 070000 （120分钟）
     * return 20171201 050000
     */
    public static String getBeforeTime(String str) {
        int mins = -120 ;
        return getAfterTimeByMins(str,mins);
    }

    /**
     * 得到指定分钟后的时间 传入 str类型 返回 指定分钟后的str类型 input 20171201 070000 （30分钟） return
     * 20171201 073000
     */
    public static String getAfterTimeByMins(String str,int mins) {
        Date dBefore = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
        Calendar calendar = Calendar.getInstance();
        try {
            // 把当前时间赋给日历
            calendar.setTime(format.parse(str));
        } catch (ParseException e) {
            LogManager
                    .log("E", "date format is error :" + str + e.getMessage());
        }
        // 设置为30 minute 后
        calendar.add(Calendar.MINUTE, mins);
        dBefore = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss");
        String defaultStartDate = sdf.format(dBefore);

        return defaultStartDate;
    }

    /**
     *
     * @return 20171201 073000 str 类型
     */
    public static String getCurrentTime() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss");
        String currentDate = sdf.format(new Date());

        return currentDate;
    }
}
