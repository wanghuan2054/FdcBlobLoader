package blobdata;

public class ConstantDefinition {

	public static final String SIX_CLOCK = "060000";
	public static final String ZERO_CLOCK = "000000";
	public static final String EIGHTEEN_CLOCK = "180000";
	public static final String LINE_INFO = "line_info";
	public static final String SVID_INFO = "svid_info";
	public static final String PARA_MNAME = "param_name";
	public static final String DATA_TYPE_CD = "data_type_cd";
	public static final String TIME = "time";
	public static final String LOT_ID = "lotid";
	public static final String SUBSTRATE_ID = "substrateid";
	public static final String SLOT = "slot";
	public static final String RECIPE = "recipe";
	public static final String PRODUCT = "product";
	public static final String STEP = "step";
	public static final String STEPNAME = "stepname";
	// public static final String STATUS = "status";
	public static final String RSD_04 = "rsd_04";
	public static final String RSD_05 = "rsd_05";
	public static final String RSD_17 = "rsd_17";
	public static final String EQP_MODULE_ID = "EQP_MODULE_ID";
	public static final String OPERATION_ID = "OPERATION_ID";
	public static final String FILE_DATA = "FILE_DATA";

	public static final String FLT_TYPE = "FLT";
	public static final String INT_TYPE = "rsd_17";

	public static final String LINE_SEPARATOR = "line.separator";

	public static final String DEFAULT_CONFIGFILE = "BlobLoader.properties";
	
	public static final String DEFAULT_LOGCONFIGFILE = "log4j.properties";

	// 每一条Blob解压出来的字段 ， 提取出来的数据临时存在该文件下
	public static final String TEMPFILE = "resultTemp.txt";

	public static final String EMPTYSTRING = "";

	public static final String DELEMETER_COMMA = ",";

	public static final String DELEMETER_AT = "@";

	public static final String DELEMETER_TAB = String.format("%c",
			new Object[] { Integer.valueOf(9) });
	public static final String DELEMETER_ENTER = String.format("%c%c",
			new Object[] { Integer.valueOf(13), Integer.valueOf(10) });

	public static final String DELEMETER_CIRCUMFLEX = "^";

	public static final String DELEMETER_TILDE = "~";

	public static final String DELEMETER_EQUAL = "=";
	public static final String LEFT_SLASH = "/";

	public static final String DELEMETER_SPACE = String.format("%c",
			new Object[] { Integer.valueOf(32) });

	public static final int BUFFER_SIZE = 1024;

	// public static void main(String args[]) {
	// System.out.println(ConstantDefinition.DELEMETER_AT);
	// }
}
