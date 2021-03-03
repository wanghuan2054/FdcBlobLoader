package blobdata;

public class RSDData {
	// RSD_04
	private String moduleName;
	// RSD_05 Production Type 目前无法从EQP_TRACE_TRX_FDC表的相关字段得到，暂时不启用这个属性
	private String productionType;
	//  RSD_17
	private String operationName;

	public RSDData() {
		super();
	}

	public RSDData(String moduleName, String productionType,
			String operationName) {
		this.moduleName = moduleName;
		this.productionType = productionType;
		this.operationName = operationName;
	}

	public RSDData(String moduleName, String operationName) {
		this.moduleName = moduleName;
		this.operationName = operationName;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getProductionType() {
		return productionType;
	}

	public void setProductionType(String productionType) {
		this.productionType = productionType;
	}

	public String getOperationName() {
		return operationName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

}
