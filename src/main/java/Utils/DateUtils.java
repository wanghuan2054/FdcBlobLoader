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
	 * timeStampToString: convert timestamp to string timekey
	 * @author 10024908 
	 * @return converted timekey
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
     * Get time after specified minutes
     * Input str type, return str type after specified minutes
     * input 20171201 070000 (30 minutes) return 20171201 073000
     */
    public static String getAfterTime(String str) {
//      Calculate EndTime forward, default 30 minutes later
        int mins = 30 ;
        return getAfterTimeByMins(str,mins);
    }
    /**
     * Get time before specified minutes
     * Input str type, return str type before specified minutes
     * input 20171201 070000 (120 minutes) return 20171201 050000
     */
    public static String getBeforeTime(String str) {
        int mins = -120 ;
        return getAfterTimeByMins(str,mins);
    }

    /**
     * Get time after specified minutes
     * Input str type, return str type after specified minutes
     * input 20171201 070000 (30 minutes) return 20171201 073000
     */
    public static String getAfterTimeByMins(String str,int mins) {
        Date dBefore = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
        Calendar calendar = Calendar.getInstance();
        try {
            // Assign current time to calendar
            calendar.setTime(format.parse(str));
        } catch (ParseException e) {
            LogManager
                    .log("E", "date format is error :" + str + e.getMessage());
        }
        // Set to 30 minutes later
        calendar.add(Calendar.MINUTE, mins);
        dBefore = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss");
        String defaultStartDate = sdf.format(dBefore);

        return defaultStartDate;
    }

    /**
     *
     * @return 20171201 073000 str type
     */
    public static String getCurrentTime() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss");
        String currentDate = sdf.format(new Date());

        return currentDate;
    }
}
