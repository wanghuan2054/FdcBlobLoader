package blobData;

public enum EDS_FDC_TRACEFIELD {

	//
	SATRT_DTTS("Start_DTTS", 0), TIME("time", 1), LOT_ID("lotid", 2), SUBSTRATE_ID(
			"substrateid", 3), SLOT("slot", 4), RECIPE("recipe", 5), PRODUCT(
			"product", 6), STEP("step", 7), STEPNAME("stepname", 8), RSD_04(
			"rsd_04", 9), RSD_05("rsd_05", 10), RSD_17("rsd_17", 11);

	private String name;
	private int index;

	private EDS_FDC_TRACEFIELD(String name, int index) {
		this.name = name;
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
