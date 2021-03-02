package blobData;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import config.LogManager;

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
	
}
