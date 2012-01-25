package me.smickles.DynamicMarket;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name = "DynaMark_Commodities")
public class Commodities {
    
    @Id
    private int id;
    @NotEmpty
    private String name;
    @NotNull
    private double value;
    @NotNull
    private int number;
    @NotNull
    private double maxValue;
    @NotNull 
    private double minValue;
    @NotNull
    private double changeRate;
	@NotNull
    private int data;
    @NotNull
    private double spread;
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public double getValue() {
        return value;
    }
    public void setValue(double value) {
        this.value = value;
    }
    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }
    public double getMaxValue() {
        return maxValue;
    }
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }
    public double getMinValue() {
        return minValue;
    }
    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }
    public double getChangeRate() {
        return changeRate;
    }
    public void setChangeRate(double changeRate) {
        this.changeRate = changeRate;
    }
    public int getData() {
        return data;
    }
    public void setData(int data) {
        this.data = data;
    }
    public double getSpread() {
        return spread;
    }
    public void setSpread(double spread) {
        this.spread = spread;
    }
    @Override
    public String toString() {
    	return "id " + id
    			+ " name " + name
    			+ " number " + number
    			+ " data " + data
    			+ " value " + value
    			+ " minvalue " + minValue
    			+ " maxvalue " + maxValue
    			+ " cangerate " + changeRate
    			+ " spread " + spread;
    }
}
