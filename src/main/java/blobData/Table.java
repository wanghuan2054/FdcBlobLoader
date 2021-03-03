package blobdata;

import java.util.ArrayList;
import java.util.List;

public class Table {
 
	private List<String> fields; // 存放常规字段和参数字段的列表
	private List<ArrayList<String>> tableRecords; // 存储所有参数信息表

	public Table() {
	}

	public List<String> getFields() {
		return fields; 
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	public List<ArrayList<String>> getTableRecords() {
		return tableRecords;
	}

	public void setTableRecords(List<ArrayList<String>> tableRecords) {
		this.tableRecords = tableRecords;
	}

	/*
	 * fields 列表中分割出共有字段 返回每张Glass共有的信息字段 得到最后一个共有字段的索引
	 */
	public int getLastIndexOfComFields() {
		return EDS_FDC_TRACEFIELD.RSD_17.getIndex();
	}

	public int getStartIndexOfParams() {
		return getLastIndexOfComFields() + 1;
	}

	public int getLengthOfFields() {
		return fields.size();
	}

	public int getNumsOfParams() {
		return getLengthOfFields() - getStartIndexOfParams();
	}

	/*
	 * fields 列表中分割每张Glass上传的参数名称列表 返回 参数名称列表
	 */
	public List<String> getParams() {

		List<String> list = new ArrayList<String>();
		for (int i = getStartIndexOfParams(); i < getLengthOfFields(); i++) {
			list.add(fields.get(i));
		}

		return list;
	}
}
