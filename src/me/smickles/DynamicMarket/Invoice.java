package me.smickles.DynamicMarket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Invoice {
	public BigDecimal value = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
	public BigDecimal total = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
	
	/**
	 * Create an Invoice for the purchase or sale of an item
	 * @param initValue the beginning value of the item 
	 * @param initTotal the beginning total of of the Invoice
	 */
	public Invoice(BigDecimal initValue, BigDecimal initTotal) {
		value = initValue;
		total = initTotal;
	}
	
	/**
	 * Get the current proposed value of the item in question 
	 * @return current proposed value of the item
	 */
	public BigDecimal getValue() {
		return value;
	}
	
	/**
	 * Get the current total of the Invoice
	 * @return current total of the Invoice
	 */
	public BigDecimal getTotal() {
		return total;
	}
	
	/**
	 * Add an amount to the total of the invoice
	 * @param addend the amount to be added to the total.
	 */
	public void addTotal(BigDecimal addend) {
		total = total.add(addend);
	}
	
	/**
	 * Add an amount to the proposed value of the item
	 * @param addend the amount to be added
	 */
	public void addValue(BigDecimal addend) {
		value = value.subtract(addend);
	}
	
	/**
	 * Subtract an amount from the proposed value of the item 
	 * @param subtrahend the amount to be subtracted
	 */
	public void subtractValue(BigDecimal subtrahend) {
		value = value.subtract(subtrahend);
	}

}
