package blobData;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import config.LogManager;

public class FDCTraceParserBlob {

	private DBUtil dbUtil = DBUtil.getInstance();

	/*
	 * 获取step中的开始step 索引 ，即从每一秒上传的 trace data 中获取step开始步骤 去除step
	 * 长度=0，就是Step为none情况 , 去除step 为0.0的情况
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
	 * 获取step中的结尾step 索引 ，即从每一秒上传的 trace data 中获取step结束最后一步步骤
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
	 * 从tempTxt中读取内容，并生成数据库表存储格式 返回整张表存在内存中 List<ArrayList<String>> Table
	 * 中存储表列字段 和所有记录
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
	 * 按照step分组统计每个参数的min max avg 从上述生成的table中 找到step的开始和结束索引,
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
	 * 按照time 分组统计每个参数的min max avg 从上述生成的table中 找到step的开始和结束索引,
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
	 * 给定开始索引和结束索引，还有 table 查询在索引段之间所有参数的 min max avg
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
			ArrayList<String> arr1 = tableList.get(begin); // 取这一分组后的第一条记录的公共字段
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
				if (isNumeric(arr.get(i))) { // 若参数中出现非数字参数，直接跳过不参与计算
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
	 * 判断是否为浮点数
	 * 
	 * @param str 传入的字符串
	 * 
	 * @return 是浮点数返回true,否则返回false
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
	 * 按照step and time 进行groupBy 分组之和计算每张玻璃在每一个Step 和 time(班次)之间 ，
	 * 参数名、参数的最大、最小值，以及平均值 得到上述四个值后，并得到这个分组开始和结束时间，拿到公共字段如：Lotid glassId
	 * recipe等写入数据库
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
			// continue 判断一个Glass的开始班次和结束班次
			// start = end 属于一个班次 只计算 step 索引
			// start ！= end 即不属于同一个班次 需要计算step 和 time 索引
			stepRangeIndex = getStepIndexShift(i, tableList);
			timeRangeIndex = getTimeIndexShift(i, tableList);
			if (((i + timeRangeIndex) == (cnt - 1))) { // 所有时间属于一个班次
				// 按照实际的 step 范围计算
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
			} else { // 不属于一个班次
				if (stepRangeIndex > timeRangeIndex) {
					// 分AB班 两个班次处理
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

	/* 测试时候使用
	 * 对聚合之后的结果写到数据库之前 ， 打印输出到屏幕 result中保存结果数据的属性字段，以及结果数据行 Start_DTTS End_DTTS
	 * time lotid substrateid slot recipe product step stepname rsd_04 rsd_05
	 * L5300 以及所有參數
	 */
	public void displayEveryGroup(
			LinkedHashMap<Vector<String>, List<AggregateFunction>> result) {

		Iterator<Entry<Vector<String>, List<AggregateFunction>>> iter = result
				.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
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
