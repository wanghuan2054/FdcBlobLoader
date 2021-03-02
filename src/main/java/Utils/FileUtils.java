package Utils;

import blobData.ConstantDefinition;
import blobData.RSDData;
import config.LogManager;

import java.io.*;
import java.sql.Blob;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class FileUtils {

	private static LinkedHashMap<String, List<String>> allParams;

	/*
	 * 从数据获取Blob字段， 并按照GZip解压缩到outPath临时路径下
	 */
	public static void decompressionGZip(Blob blob, String outPath) {

		FileOutputStream out = null;
		InputStream in = null;
		GZIPInputStream gis = null;
		int count = 0;
		File file = new File(outPath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				LogManager.log("E",
						"create file fail" + file.getAbsolutePath());
			}
		}
		try {
			out = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			LogManager.log("E",
					"file output flow open fail " + file.getAbsolutePath());
		}
		try {
			in = blob.getBinaryStream();
			gis = new GZIPInputStream(in);
			byte data[] = new byte[ConstantDefinition.BUFFER_SIZE];
			while ((count = gis.read(data, 0, ConstantDefinition.BUFFER_SIZE)) != -1) {
				out.write(data, 0, count);
			}
			gis.close();
			in.close();
			out.close();
		} catch (Exception ex) {
			LogManager.log("E",
					"File Flow closed error." + ex.getMessage());
		}
	}

	/*
	 * 从临时文件resultTemp下取出解压完的Blob数据
	 */
	public static LinkedHashMap<String, List<String>> getContentFromTemptxt(
            RSDData rsdData, String path) {
		BufferedReader bre = null;
		String tempLine_str = null;
		List<String> lineInfo = new ArrayList<String>();
		List<String> svidInfo = new ArrayList<String>();
		List<String> paramName = new ArrayList<String>();
		List<String> dataTypeCD = new ArrayList<String>();
		List<String> tableHeader = new ArrayList<String>();
		allParams = new LinkedHashMap<String, List<String>>();
		try {
			bre = new BufferedReader(new FileReader(new File(path)));// 此时获取到的bre就是整个文件的缓存流
			String params_Keyname = null;
			while ((tempLine_str = bre.readLine()) != null) // 判断最后一行不存在，为空结束循环
			{
				String[] lineList = null;
				if (tempLine_str.contains(ConstantDefinition.LINE_INFO)) {
					lineList = tempLine_str
							.split(ConstantDefinition.DELEMETER_EQUAL)[1]
							.split(ConstantDefinition.DELEMETER_TAB);
					lineInfo = new ArrayList(Arrays.asList(lineList));
					allParams.put(tempLine_str
							.split(ConstantDefinition.DELEMETER_EQUAL)[0],
							lineInfo);
					continue;
				} else if (tempLine_str.contains(ConstantDefinition.SVID_INFO)) {
					lineList = tempLine_str
							.split(ConstantDefinition.DELEMETER_EQUAL)[1]
							.split(ConstantDefinition.DELEMETER_TAB);
					svidInfo = new ArrayList(Arrays.asList(lineList));
					continue;
				} else if (tempLine_str.contains(ConstantDefinition.PARA_MNAME)) {
					lineList = tempLine_str
							.split(ConstantDefinition.DELEMETER_EQUAL)[1]
							.split(ConstantDefinition.DELEMETER_TAB);
					params_Keyname = tempLine_str
							.split(ConstantDefinition.DELEMETER_EQUAL)[0];
					paramName = new ArrayList(Arrays.asList(lineList));
					continue;
				} else if (tempLine_str
						.contains(ConstantDefinition.DATA_TYPE_CD)) {
					lineList = tempLine_str
							.split(ConstantDefinition.DELEMETER_EQUAL)[1]
							.split(ConstantDefinition.DELEMETER_TAB);
					dataTypeCD = new ArrayList(Arrays.asList(lineList));
					paramName = getRealParamsBySourceAndTarget(paramName,
							svidInfo, dataTypeCD);
					allParams.put(params_Keyname, paramName);
					continue;
				} else if (!(hasContainsHeader(tempLine_str, lineInfo))
						.isEmpty()) {

					lineList = tempLine_str.split(
							ConstantDefinition.DELEMETER_TAB, -1);
					if (lineList[0].equals(ConstantDefinition.RSD_17)) { // "rsd_17"是
																			// operationIDList，只留取一个StepID即可
						lineList = getSingleFromList(tempLine_str);
						for (int i = 0; i < lineList.length; i++) {
							if (rsdData.getOperationName() != null) {
								// if (isIllegalStr(lineList[i])) {
								lineList[i] = rsdData.getOperationName();
								// }
							}
						}
					} else if (lineList[0].equals(ConstantDefinition.TIME)) {

						// 先保留原始的time ，没有进行过AB班转换的
						tableHeader = new ArrayList(Arrays.asList(lineList));
						tableHeader.remove(0);
						allParams.put("Start_DTTS", tableHeader);

						lineList = makeTimeToAB(lineList);
					} else if (lineList[0].equals(ConstantDefinition.RSD_04)) {

						for (int i = 1; i < lineList.length; i++) {
							if (rsdData.getModuleName() != null) {
								// if (isIllegalStr(lineList[i]))
								lineList[i] = rsdData.getModuleName();
							}
						}
					} else if (lineList[0].equals(ConstantDefinition.RSD_05)) {

						for (int i = 1; i < lineList.length; i++) {
							if (rsdData.getProductionType() != null) {
								// if (isIllegalStr(lineList[i]))
								lineList[i] = rsdData.getProductionType();
							}
						}
					}
					tableHeader = new ArrayList(Arrays.asList(lineList));
					tableHeader.remove(0);
					allParams.put(lineList[0], tableHeader);
				} else if (!(hasContainsHeader(tempLine_str, paramName))
						.isEmpty()) {
					lineList = tempLine_str.split(
							ConstantDefinition.DELEMETER_TAB, -1);
					tableHeader = deleteAt(new ArrayList(
							Arrays.asList(lineList)));
					allParams.put(lineList[0], tableHeader);
				}
			}
		} catch (FileNotFoundException e) {
			LogManager.log("E",
					"resultTemp file not found exception" + e.getMessage());

		} catch (IOException e) {
			LogManager.log("E",
					"resultTemp IO error" + e.getMessage());

		}

		return allParams;
	}

	/*
	 * 判断解压出来的 moduleId productionType operationId 是否为空 为一下函数中几种状态 return true
	 * ，否则返回false input : 需要判断的字符串
	 */
	public static Boolean isIllegalStr(String str) {

		str = str.trim();
		if (str == null || str.length() == 0 || str.equals("NaN")
				|| str.equals("nan") || str.equals("NAN")
				|| str == ConstantDefinition.EMPTYSTRING) {
			return true;
		}
		return false;
	}

	/*
	 * 将SVID――info中参数列表与param_name
	 * 对应，找出svid中编号全为数字参数下标，并在param_name中只保留这些下标位置的参数 同时判断dataTypeCd
	 * 中参数列表与param_name参数进行对应，去除str类型 ，只保留INT 和 FLT类型参数
	 */
	public static List<String> getRealParamsBySourceAndTarget(
			List<String> paramsList, List<String> svidList,
			List<String> dataTypeCD) {
		int len = svidList.size();
		List<String> tempList = new ArrayList<String>();
		for (int i = 0; i < len; i++) {
			if (isDigit(svidList.get(i))) {
				if (dataTypeCD.get(i).equals(ConstantDefinition.INT_TYPE)
						|| dataTypeCD.get(i)
								.equals(ConstantDefinition.FLT_TYPE))
					tempList.add(paramsList.get(i));
			}
		}
		return tempList;
	}

	// 判断一个字符串是否都为数字
	public static boolean isDigit(String strNum) {
		Pattern pattern = Pattern.compile("[0-9]{1,}");
		Matcher matcher = pattern.matcher((CharSequence) strNum);
		return matcher.matches();
	}

	/*
	 * 将获取到的Time参数 转换成A B 班的标准时间数据标记 2017-11-10 09:05:35.700 -----> 20171110
	 * 060000 2017-11-10 18:05:35.700 -----> 20171110 180000
	 */
	public static String[] makeTimeToAB(String[] lineInfo) {

		String[] strInfo = lineInfo;
		int len = strInfo.length;
		String tempStr = null;
		for (int i = 1; i < len; i++) {
			strInfo[i] = compare_date(strInfo[i]);
		}
		return strInfo;
	}

	/*
	 * String date = "2017-11-10 09:05:35.700"; 返回为 20171110 090535格式，为了判断A班 B班
	 */
	public static String parseDate(String date) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat format1 = new SimpleDateFormat("yyyyMMdd HHmmss");
		String newDate = null;
		try {
			newDate = format1.format(format.parse(date));
		} catch (ParseException e) {
			LogManager.log("E",
					"date parse error" + date + " " + e.getMessage());
		}
		return newDate;
	}

	/*
	 * 得到指定时间的前一天 传入 Date类型 返回 一天前的Date类型
	 */
	public static String getBeforeDate(String str) {

		Date dBefore = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance(); // 得到日历
		try {
			calendar.setTime(format.parse(str)); // 把当前时间赋给日历
		} catch (ParseException e) {
			LogManager.log("E",
					"date format is error " + str + " " + e.getMessage());
		}
		calendar.add(Calendar.DAY_OF_MONTH, -1); // 设置为前一天
		dBefore = calendar.getTime(); // 得到前一天的时间

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd"); // 设置时间格式
		String defaultStartDate = sdf.format(dBefore); // 格式化前一天

		return defaultStartDate;
	}

	/*
	 * 
	 * 
	 * */
	public static String compare_date(String date) {
		// 20171110 090535 ,时间开始坐标位置是 9
		int beginIndex = 9;
		date = parseDate(date);
		String time = date.substring(beginIndex); // 截取时间
		String newDate = null;
		/*
		 * 如果当前time >= 060000 并且 < 180000 则属于六点班次 如果当前time >= 180000 则属于18点班次
		 * 如果当前time < 060000 并且 < 180000 则属于六点班次
		 */
		if (time.compareTo(ConstantDefinition.SIX_CLOCK) >= 0
				&& time.compareTo(ConstantDefinition.EIGHTEEN_CLOCK) < 0) {
			newDate = date.substring(0, 8).concat(
					ConstantDefinition.DELEMETER_SPACE
							+ ConstantDefinition.SIX_CLOCK);
		} else if (time.compareTo(ConstantDefinition.EIGHTEEN_CLOCK) >= 0) {
			newDate = date.substring(0, 8).concat(
					ConstantDefinition.DELEMETER_SPACE
							+ ConstantDefinition.EIGHTEEN_CLOCK);
		} else if (time.compareTo(ConstantDefinition.ZERO_CLOCK) >= 0
				&& time.compareTo(ConstantDefinition.SIX_CLOCK) < 0) {
			newDate = getBeforeDate(date.substring(0, 8)).concat(
					ConstantDefinition.DELEMETER_SPACE
							+ ConstantDefinition.EIGHTEEN_CLOCK);
		}

		return newDate;
	}

	/*
	 * 提取rsd_17参数 ， 也可以提取所有用逗号隔开的参数列表 返回每个子参数列表中，全部相同值仅保留一个参数
	 * L5300,L5300,L5300,L5300 L5300,L5300,L5300,L5300 rsd_17
	 * operationIDList，只留取一个StepID即可
	 */
	public static String[] getSingleFromList(String lineInfo) {

		String[] strInfo = lineInfo.split(ConstantDefinition.DELEMETER_TAB, -1);
		int len = strInfo.length;
		String tempStr = null;
		for (int i = 0; i < len; i++) {
			tempStr = strInfo[i];
			if (tempStr.contains(ConstantDefinition.DELEMETER_COMMA)) {
				strInfo[i] = tempStr.split(ConstantDefinition.DELEMETER_COMMA,
						-1)[0];
			}
		}

		return strInfo;
	}

	/*
	 * 判断提取出来的一行内容中，第一个关键字是否存在于line_info列表中 存在返回true ， 不存在返回false
	 */
	public static String hasContainsHeader(String lineStr, List<String> list) {

		String[] stopCols = { "rsd_01", "rsd_02", "rsd_03", "rsd_06", "rsd_07",
				"rsd_08", "rsd_09", "rsd_10", "rsd_11", "rsd_12", "rsd_13",
				"rsd_14", "rsd_15", "rsd_16", "rsd_18", "rsd_19", "rsd_20",
				"status" };
		List<String> stopWordsList = new ArrayList<String>(
				Arrays.asList(stopCols));
		list.removeAll(stopWordsList); // 去除 stopCols 列出的列名所包含的数据
		String header = null;
		lineStr = lineStr.split(ConstantDefinition.DELEMETER_TAB)[0].trim();
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			header = it.next().trim();
			if (lineStr.equals(header)) {
				return header;
			}
		}
		return ConstantDefinition.EMPTYSTRING;
	}

	/*
	 * 0.956@ 1.957@ 去除traceParameter中每个参数后面的@字符， 返回去除@字符之后的参数列表
	 */
	public static List<String> deleteAt(List<String> list) {
		String result = null;
		String singleStr = null;
		List<String> ll = new ArrayList<String>();
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			singleStr = it.next();
			// if 判断single参数串中是否含有@ ， 可以有效的去除第一个名字参数 例如：CHC_RECIPE_ID等
			if (singleStr.contains(ConstantDefinition.DELEMETER_AT)) {
				if (singleStr.length() == 1) {
					result = ConstantDefinition.EMPTYSTRING;
				} else {
					// 若参数给出目标值后@后面跟着多个SPEC，舍弃@后spec内容
					result = singleStr.split(ConstantDefinition.DELEMETER_AT)[0];
				}
				ll.add(result);
			}
		}
		return ll;
	}
}
