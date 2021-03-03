package blobdata;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AggregateFunction {

	private String itemName;
	private double minValue;
	private double maxValue;
	private double avgValue;

	public double decimalPointFormat(double value) {
		return new BigDecimal(value).setScale(10, RoundingMode.HALF_UP)
				.doubleValue();
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public double getMinValue() {
		return minValue;
	}

	public void setMinValue(double minValue) {
		// this.minValue = decimalPointFormat(minValue);
		this.minValue = minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public void setMaxValue(double maxValue) {
		// this.maxValue = decimalPointFormat(maxValue);
		this.maxValue = maxValue;
	}

	public double getAvgValue() {
		return avgValue;
	}

	public void setAvgValue(double avgValue) {
		// this.avgValue = decimalPointFormat(avgValue);
		this.avgValue = avgValue;
	}

	public AggregateFunction() {
	}

}
