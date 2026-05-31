package blobdata;

import Utils.DBUtil;
import Utils.FileUtils;
import config.LogManager;

import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FDCTraceParserBlob {

	private DBUtil dbUtil = DBUtil.getInstance();

	/*
	 * Get the start step index from step, i.e., get the step start step from trace data uploaded every second
	 * Remove step length=0 (step is none case), remove step as 0.0 case
	 */
	public int getBeginIndexOfStep(LinkedHashMap<String, List<String>> hmParam2) {
		Iterator<Entry<String, List<String>>> iter = hmParam2.entrySet()
				.iterator();

		int beginIndex = 0;
		while (iter.hasNext()) {
			Entry<String, List<String>> paramKey = iter.next();
			List<String> paramValues = hmParam2.get(paramKey.getKey());
			if (paramKey.getKey().contains(ConstantDefinition.STEP)) {
				for (int i = 0; i < paramValues.size(); i++) {
					if (paramValues.get(i).trim().length() != 0
							&& !paramValues.get(i).trim().equals("0.0")) {
						beginIndex = i;
						return beginIndex;
					}
				}
			}
		}
		return beginIndex;
	}

	/*
	 * Get the end step index from step, i.e., get the step end last step from trace data uploaded every second
	 */
	public int getEndIndexOfStep(LinkedHashMap<String, List<String>> hmParam2) {
		Iterator<Entry<String, List<String>>> iter = hmParam2.entrySet()
				.iterator();
		int endIndex = 0;
		while (iter.hasNext()) {
			Entry<String, List<String>> paramKey = iter.next();
			List<String> paramValues = hmParam2.get(paramKey.getKey());
			endIndex = paramValues.size();
			if (paramKey.getKey().contains(ConstantDefinition.STEP)) {
				for (int i = paramValues.size() - 1; i >= 0; i--) {
					if (paramValues.get(i).length() != 0
							&& !paramValues.get(i).equals("0.0")) {
						endIndex = i + 1;
						return endIndex;
					}
				}
			}
		}
		return endIndex;
	}

	public List<String> getLinkMapParamList(
			LinkedHashMap<String, List<String>> linkMap) {
		Iterator<Entry<String, List<String>>> iter = linkMap.entrySet()
				.iterator();
		List<String> header1 = new ArrayList<String>();
		while (iter.hasNext()) {
			Entry<String, List<String>> paramKey = iter.next();
			if (!paramKey.getKey().equals(ConstantDefinition.LINE_INFO)
					&& !paramKey.getKey().equals(ConstantDefinition.PARA_MNAME)) {
				header1.add(paramKey.getKey());
			}
		}
		return header1;
	}

	/*
	 * Read content from tempTxt and generate database table storage format
	 * Return the entire table stored in memory List<ArrayList<String>> Table storing table column fields and all records
	 */
	public Table createTable(ResultSet rs) {

		Boolean flag;
		RSDData rsdData = dbUtil.parserBlobData(rs);
		LinkedHashMap<String, List<String>> hmParam;
		hmParam = new LinkedHashMap<String, List<String>>();
		Table table = new Table();
		if (rsdData == null) {
			// System.out.print("get blob data from database is success.  ");
			LogManager.log("E","database rsd data is empty , parserBlobData return null ");
			
			return null;
		}
		hmParam = FileUtils.getContentFromTemptxt(rsdData,
				ConstantDefinition.TEMPFILE);
		Iterator<Entry<String, List<String>>> iter = null;
		int beginStep = getBeginIndexOfStep(hmParam);
		int endStep = getEndIndexOfStep(hmParam);
		List<String> header = getLinkMapParamList(hmParam);
		table.setFields(header);

		List<ArrayList<String>> tableList = new ArrayList<ArrayList<String>>();
		ArrayList<String> singleRecord = null;
		boolean sign = true;
		for (int i = beginStep; i < endStep; i++) {
			iter = hmParam.entrySet().iterator();
			flag = true;
			singleRecord = new ArrayList<String>();
			while (iter.hasNext()) {
				Entry<String, List<String>> paramKey = iter.next();
				if (paramKey.getKey().equals(ConstantDefinition.LINE_INFO)
						|| paramKey.getKey().equals(
								ConstantDefinition.PARA_MNAME)) {
					continue;
				}
				List<String> paramValues = hmParam.get(paramKey.getKey());

				singleRecord.add(paramValues.get(i));
			}
			tableList.add(singleRecord);
		}
		table.setTableRecords(tableList);
		return table;
	}

	/*
	 * Group by step and calculate min max avg for each parameter
	 * Find step start and end index from the generated table
	 */
	public int getStepIndexShift(int startIndex,
			List<ArrayList<String>> tableList) {

		int cnt = tableList.size();
		int stepIndex = EDS_FDC_TRACEFIELD.STEP.getIndex();
		String stepValue = null;
		int endIndex = startIndex;
		stepValue = tableList.get(startIndex).get(stepIndex);
		for (int j = startIndex + 1; j < cnt; j++) {
			if (stepValue.equals(tableList.get(j).get(stepIndex))) {
				endIndex = j;
			} else {
				break;
			}
		}
		return endIndex - startIndex;
	}

	/*
	 * Group by time and calculate min max avg for each parameter
	 * Find step start and end index from the generated table
	 */
	public int getTimeIndexShift(int startIndex,
			List<ArrayList<String>> tableList) {

		int cnt = tableList.size();
		int timeIndex = EDS_FDC_TRACEFIELD.TIME.getIndex();
		String stepValue = null;
		int endIndex = startIndex;
		stepValue = tableList.get(startIndex).get(timeIndex);
		for (int j = startIndex + 1; j < cnt; j++) {
			if (stepValue.equals(tableList.get(j).get(timeIndex))) {
				endIndex = j;
			} else {
				break;
			}
		}
		return endIndex - startIndex;
	}

	/*
	 * Given start index and end index, and table
	 * Query min max avg for all parameters between index range
	 */
	public LinkedHashMap<Vector<String>, List<AggregateFunction>> computeAggFuction(
			Table table, int begin, int end) {

		LinkedHashMap<Vector<String>, List<AggregateFunction>> finalResult = new LinkedHashMap<Vector<String>, List<AggregateFunction>>();
		List<AggregateFunction> newList = new ArrayList<AggregateFunction>();
		AggregateFunction aggregateFunction = null;
		List<ArrayList<String>> tableList = table.getTableRecords();
		List<String> fields = table.getFields();

		// for (String s : fields) {
		// System.out.printf("%s ", s);
		// }
		// System.out.println();

		ArrayList<String> arr = null;
		List<Double> nums = null;
		double sum;
		end = begin + end;
		StringBuilder sBuilder = new StringBuilder();
		Vector<String> vector = new Vector<String>();
		for (int i = 0; i <= table.getLastIndexOfComFields(); i++) {
			ArrayList<String> arr1 = tableList.get(begin); // Get common fields from first record of this group
			if (i == 0) {
				// sBuilder.append(arr1.get(i));
				vector.add(arr1.get(i));
			} else {
				// sBuilder.append(ConstantDefinition.DELEMETER_TAB +
				// arr1.get(i));
				vector.add(arr1.get(i));
			}
		}

		Boolean endDTTSBoolean = true;
		for (int i = table.getStartIndexOfParams(); i < table
				.getLengthOfFields(); i++) {
			aggregateFunction = new AggregateFunction();
			aggregateFunction.setItemName(fields.get(i));
			sum = 0.0;
			nums = new ArrayList<Double>();
			for (int j = begin; j <= end; j++) {
				arr = tableList.get(j);
				if (endDTTSBoolean && j == end) {
					vector.insertElementAt(arr.get(0), 1);
					endDTTSBoolean = false;
				}
				if (isNumeric(arr.get(i))) { // If non-numeric parameter appears, skip it from calculation
					nums.add(Double.valueOf(arr.get(i)));
					sum += Double.valueOf(arr.get(i));
				} else {
					continue;
				}
			}
			if (!nums.isEmpty()) {
				aggregateFunction.setMinValue(Collections.min(nums));
				aggregateFunction.setMaxValue(Collections.max(nums));
				aggregateFunction.setAvgValue(sum / (end - begin + 1));
				newList.add(aggregateFunction);
			}
		}
		finalResult.put(vector, newList);
		return finalResult;
	}

	/*
	 * Check if the string is a floating point number
	 * 
	 * @param str input string
	 * 
	 * @return true if floating point number, otherwise false
	 */
	public boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("-?[0-9]*.?[0-9]+");
		Matcher isNum = pattern.matcher(str);
		if (!isNum.matches()) {
			return false;
		}
		return true;
	}

	/*
	 * Group by step and time, then calculate parameter name, max, min, and avg values
	 * for each glass between each Step and time (shift)
	 * After getting above four values, get group start and end time, fetch common fields like: Lotid glassId recipe etc. and write to database
	 */
	public void groupByStepAndTime(Table table) {

		LinkedHashMap<Vector<String>, List<AggregateFunction>> finalResults = new LinkedHashMap<Vector<String>, List<AggregateFunction>>();
		List<ArrayList<String>> tableList = table.getTableRecords();
		List<AggregateFunction> newList = null;
		int stepRangeIndex;
		int timeRangeIndex;
		int cnt = tableList.size();
		int i = 0;
		while (i < cnt) {
			// continue: determine start shift and end shift of a Glass
			// start = end: belongs to same shift, only calculate step index
			// start != end: not same shift, need to calculate both step and time index
			stepRangeIndex = getStepIndexShift(i, tableList);
			timeRangeIndex = getTimeIndexShift(i, tableList);
			if (((i + timeRangeIndex) == (cnt - 1))) { // All times belong to same shift
				// Calculate by actual step range
				finalResults = computeAggFuction(table, i, stepRangeIndex);
				// displayEveryGroup(finalResults);
				// new Thread(new Runnable() {
				//
				// @Override
				// public void run() {
				// DBUtil.getInstance().writeToMDW(finalResults);
				// }
				// }).start();
				DBUtil.getInstance().writeToMDW(finalResults);
				i += (stepRangeIndex + 1);
			} else { // Not same shift
				if (stepRangeIndex > timeRangeIndex) {
					// Process A and B shifts separately
					finalResults = computeAggFuction(table, i, timeRangeIndex);
					// displayEveryGroup(finalResults);
					DBUtil.getInstance().writeToMDW(finalResults);
					i += (timeRangeIndex + 1);
				} else {
					finalResults = computeAggFuction(table, i, stepRangeIndex);
					// displayEveryGroup(finalResults);
					DBUtil.getInstance().writeToMDW(finalResults);
					i += (stepRangeIndex + 1);
				}
			}
		}
	}

	/* Used during testing
	 * Before writing aggregated results to database, print to screen
	 * result stores result data attribute fields and result data rows
	 * Start_DTTS End_DTTS time lotid substrateid slot recipe product step stepname rsd_04 rsd_05 L5300 and all parameters
	 */
	public void displayEveryGroup(
			LinkedHashMap<Vector<String>, List<AggregateFunction>> result) {

		Iterator<Entry<Vector<String>, List<AggregateFunction>>> iter = result
				.entrySet().iterator();
		while (iter.hasNext()) {
			Entry entry = (Entry) iter.next();
			Vector<String> paramKey = (Vector<String>) entry.getKey();
			List<AggregateFunction> paramValues = (List<AggregateFunction>) entry
					.getValue();

			AggregateFunction aggregateFunction = new AggregateFunction();

			for (int j = 0; j < paramValues.size(); j++) {
				for (int i = 0; i < paramKey.size(); i++) {
					System.out.printf("%s ", paramKey.get(i));
				}
				aggregateFunction = paramValues.get(j);
				System.out.printf("%s %s %s %s\n",
						aggregateFunction.getItemName(),
						aggregateFunction.getMinValue(),
						aggregateFunction.getMaxValue(),
						aggregateFunction.getAvgValue());
			}
		}
	}

}
