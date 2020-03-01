/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.michael.framework;

/**
 *
 * @author michael
 */
public class IntervalExpression implements Criteria {

    private final String propertyName;
    private final String tableName;
    private final Object min;
    private final Object max;

    public IntervalExpression(String tableName, String propertyName, Object min, Object max) {
        this.propertyName = propertyName;
        this.min = min;
        this.max = max;
        this.tableName = tableName;
    }

    public Object getMin() {
        return min;
    }

    public Object getMax() {
        return max;
    }

    @Override
    public String toSqlString() {
        return tableName+"."+propertyName +  " BETWEEN ? AND ?"; 
    }
}
