import config.LogManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class test {

	/** 
	 * main:(这里用一句话描述这个方法的作用). 
	 * void.
	 * @author 10024908 
	 * @param args 
	 */

	public static void main(String[] args) {
		System.out.println(getCurrentTime());
		System.out.println(getAfterTime("20210302 113000"));
	}

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
		calendar.add(Calendar.MINUTE, -30); // 设置为30 minute 后
		dBefore = calendar.getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss"); // 设置时间格式
		String defaultStartDate = sdf.format(dBefore);

		return defaultStartDate;
	}

	public static String getCurrentTime() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss"); // 设置时间格式
		String currentDate = sdf.format(new Date());

		return currentDate;
	}
}
