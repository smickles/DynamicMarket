/**DynaMark
 * Copyright 2011 Michael Carver
 * 
 * This file is part of DynaMark.
 *
 *  DynaMark is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DynaMark is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with DynaMark.  If not, see <http://www.gnu.org/licenses/>.
 **/

package me.smickles.DynamicMarket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Invoice {
    public BigDecimal value = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
    public BigDecimal total = BigDecimal.valueOf(0).setScale(2, RoundingMode.HALF_UP);
    
    /**
     * Create an Invoice for the purchase or sale of an item
     * @param initValue the beginning value of the item 
     * @param initTotal the beginning total of of the Invoice, typically 0
     */
    public Invoice(BigDecimal initValue, BigDecimal initTotal) {
        value = initValue;
        total = initTotal;
    }
    
    /**
     * Get the current proposed value of the item in question 
     * @return current proposed value of the item
     */
    public double getValue() {
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
     * subtract an amount from the total of the invoice
     * @param subtrahend the amount to be subtracted from the total.
     */
    public void subtractTotal(BigDecimal subtrahend) {
        total = total.subtract(subtrahend);
    }   
    
    /**
     * Set the total to a give value.
     * @param newTotal the desired total
     */
    public void setTotal(BigDecimal newTotal) {
        total = newTotal;        
    }
    
    /**
     * Add an amount to the proposed value of the item
     * @param addend the amount to be added
     */
    public void addValue(BigDecimal addend) {
        value = value.add(addend);
    }
    
    /**
     * Subtract an amount from the proposed value of the item 
     * @param subtrahend the amount to be subtracted
     */
    public void subtractValue(BigDecimal subtrahend) {
        value = value.subtract(subtrahend);
    }
    


}
