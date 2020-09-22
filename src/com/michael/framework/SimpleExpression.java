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
class SimpleExpression implements Criteria {
    private final String propertyName;
    private final Object value;
    private final String operator;
    private final String tableName;

    public String getOperator() {
        return operator;
    }

    public SimpleExpression(String tableName, String propertyName, Object value, String operator) {
        this.propertyName = propertyName;
        this.value = value;
        this.operator = operator;
        this.tableName = tableName;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toSqlString() {
        return tableName+"."+propertyName + " " + operator + " ?";
    }
}
