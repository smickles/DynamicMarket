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

public class Invoice {
    public double value = 0.0;
    public double total = 0.0;
    
    /**
     * Create an Invoice for the purchase or sale of an item
     * @param initValue the beginning value of the item 
     * @param initTotal the beginning total of of the Invoice, typically 0
     */
    public Invoice(double initValue, double initTotal) {
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
    public double getTotal() {
        return total;
    }
    
    /**
     * Add an amount to the total of the invoice
     * @param d the amount to be added to the total.
     */
    public void addTotal(double d) {
        total += d;
    }
    
    /**
     * subtract an amount from the total of the invoice
     * @param subtrahend the amount to be subtracted from the total.
     */
    public void subtractTotal(double subtrahend) {
        total -= subtrahend;
    }   
    
    /**
     * Set the total to a give value.
     * @param newTotal the desired total
     */
    public void setTotal(double newTotal) {
        total = newTotal;        
    }
    
    /**
     * Add an amount to the proposed value of the item
     * @param addend the amount to be added
     */
    public void addValue(double addend) {
        value += addend;
    }
    
    /**
     * Subtract an amount from the proposed value of the item 
     * @param subtrahend the amount to be subtracted
     */
    public void subtractValue(double subtrahend) {
        value -= subtrahend;
    }
    


}
