package me.smickles.DynamicMarket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Invoice extends DynamicMarket{
	public BigDecimal value = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
	public BigDecimal total = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
	
	public Invoice(BigDecimal initValue, BigDecimal initTotal) {
		value = initValue;
		total = initTotal;
	}
	
	public BigDecimal getValue() {
		return value;
	}
	
	public BigDecimal getTotal() {
		return total;
	}

}
