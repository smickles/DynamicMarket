package me.smickles.DynamicMarket;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name = "DynaMark_Commodities")
public class Commodities {
    
    @Id
    private int id;
    @NotNull
    private String name;
    @NotNull
    private BigDecimal value;
    @NotNull
    private int number;
    @NotNull
    private BigDecimal maxValue;
    @NotNull 
    private BigDecimal minValue;
    @NotNull
    private BigDecimal changeRate;
    @NotNull
    private int data;
    @NotNull
    private BigDecimal spread;
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
    public BigDecimal getValue() {
        return value;
    }
    public void setValue(BigDecimal value) {
        this.value = value;
    }
    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }
    public BigDecimal getMaxValue() {
        return maxValue;
    }
    public void setMaxValue(BigDecimal maxValue) {
        this.maxValue = maxValue;
    }
    public BigDecimal getMinValue() {
        return minValue;
    }
    public void setMinValue(BigDecimal minValue) {
        this.minValue = minValue;
    }
    public BigDecimal getChangeRate() {
        return changeRate;
    }
    public void setChangeRate(BigDecimal changeRate) {
        this.changeRate = changeRate;
    }
    public int getData() {
        return data;
    }
    public void setData(int data) {
        this.data = data;
    }
    public BigDecimal getSpread() {
        return spread;
    }
    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }
    
    

}
