package Utils;

import blobdata.ConstantDefinition;
import blobdata.RSDData;
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

	/**
	 * Get Blob field from data, and decompress via GZip to outPath temp path
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

	/**
	 * Get decompressed Blob data from temp file resultTemp
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
			// bre is the buffer stream for the entire file
			bre = new BufferedReader(new FileReader(new File(path)));
			String params_Keyname = null;
			// Loop ends when last line doesn't exist (is empty)
			while ((tempLine_str = bre.readLine()) != null)
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
					// "rsd_17" is operationIDList, keep only one StepID
					if (lineList[0].equals(ConstantDefinition.RSD_17)) {
						lineList = getSingleFromList(tempLine_str);
						for (int i = 0; i < lineList.length; i++) {
							if (rsdData.getOperationName() != null) {
								// if (isIllegalStr(lineList[i])) {
								lineList[i] = rsdData.getOperationName();
								// }
							}
						}
					} else if (lineList[0].equals(ConstantDefinition.TIME)) {

						// Keep original time first, without A/B shift conversion
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

	/**
	 * Check if decompressed moduleId, productionType, operationId are empty
	 * Return true for the following states, otherwise false
	 * input: string to check
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

	/**
	 * Match SVID-info parameter list with param_name
	 * Find indices of all-numeric parameter numbers in svid, and keep only parameters at those indices in param_name
	 * Also match dataTypeCd parameter list with param_name parameters, remove str type, keep only INT and FLT type parameters
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

	/**
	 * Check if a string consists entirely of digits
	 * @param strNum
	 * @return true or false
	 */
	public static boolean isDigit(String strNum) {
		Pattern pattern = Pattern.compile("[0-9]{1,}");
		Matcher matcher = pattern.matcher((CharSequence) strNum);
		return matcher.matches();
	}

	/**
	 * Convert Time parameter to standard A/B shift time data marker
	 * 2017-11-10 09:05:35.700 -----> 20171110 060000
	 * 2017-11-10 18:05:35.700 -----> 20171110 180000
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

	/**
	 * String date = "2017-11-10 09:05:35.700";
	 * Return 20171110 090535 format, for determining A shift or B shift
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

	/**
	 * Get the day before specified date
	 * Input Date type, return Date type one day before
	 */
	public static String getBeforeDate(String str) {

		Date dBefore = null;
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();
		try {
			calendar.setTime(format.parse(str));
		} catch (ParseException e) {
			LogManager.log("E",
					"date format is error " + str + " " + e.getMessage());
		}
		// Set to previous day
		calendar.add(Calendar.DAY_OF_MONTH, -1);
		// Get previous day time
		dBefore = calendar.getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String defaultStartDate = sdf.format(dBefore);

		return defaultStartDate;
	}


	public static String compare_date(String date) {
		// 20171110 090535, time start position is 9
		int beginIndex = 9;
		date = parseDate(date);
		// Extract time
		String time = date.substring(beginIndex);
		String newDate = null;
		/*
		 * If current time >= 060000 and < 180000, belongs to 6 o'clock shift
		 * If current time >= 180000, belongs to 18 o'clock shift
		 * If current time < 060000 and < 180000, belongs to 6 o'clock shift
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

	/**
	 * Extract rsd_17 parameter, can also extract all comma-separated parameter lists
	 * Return each sub-parameter list with only one value kept for all identical values
	 * L5300,L5300,L5300,L5300 L5300,L5300,L5300,L5300
	 * rsd_17 operationIDList, keep only one StepID
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

	/**
	 * Check if the first keyword in extracted line exists in line_info list
	 * Return true if exists, false if not
	 */
	public static String hasContainsHeader(String lineStr, List<String> list) {

		String[] stopCols = { "rsd_01", "rsd_02", "rsd_03", "rsd_06", "rsd_07",
				"rsd_08", "rsd_09", "rsd_10", "rsd_11", "rsd_12", "rsd_13",
				"rsd_14", "rsd_15", "rsd_16", "rsd_18", "rsd_19", "rsd_20",
				"status" };
		List<String> stopWordsList = new ArrayList<String>(
				Arrays.asList(stopCols));
		list.removeAll(stopWordsList); // Remove data contained in column names listed in stopCols
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

	/**
	 * 0.956@ 1.957@ Remove @ character after each parameter in traceParameter
	 * Return parameter list after @ character removal
	 */
	public static List<String> deleteAt(List<String> list) {
		String result = null;
		String singleStr = null;
		List<String> ll = new ArrayList<String>();
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			singleStr = it.next();
			// if: check if single parameter string contains @, can effectively remove first name parameter e.g., CHC_RECIPE_ID etc.
			if (singleStr.contains(ConstantDefinition.DELEMETER_AT)) {
				if (singleStr.length() == 1) {
					result = ConstantDefinition.EMPTYSTRING;
				} else {
					// If parameter has target value after @ followed by multiple SPECs, discard content after @
					result = singleStr.split(ConstantDefinition.DELEMETER_AT)[0];
				}
				ll.add(result);
			}
		}
		return ll;
	}
}
