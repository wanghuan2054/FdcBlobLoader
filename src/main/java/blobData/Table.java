package blobdata;

import java.util.ArrayList;
import java.util.List;

public class Table {
 
	private List<String> fields; // List storing regular fields and parameter fields
	private List<ArrayList<String>> tableRecords; // Table storing all parameter information

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
	 * Split common fields from fields list
	 * Return common information fields for each Glass
	 * Get index of last common field
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
	 * Split parameter name list uploaded by each Glass from fields list
	 * Return parameter name list
	 */
	public List<String> getParams() {

		List<String> list = new ArrayList<String>();
		for (int i = getStartIndexOfParams(); i < getLengthOfFields(); i++) {
			list.add(fields.get(i));
		}

		return list;
	}
}
