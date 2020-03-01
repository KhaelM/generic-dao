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
public class Restrictions {
    
    public static Criteria eq(String tableName, String propertyName, Object value) {
        SimpleExpression se = new SimpleExpression(tableName, propertyName, value, "=");
        return se;
    }

    public static Criteria ne(String tableName, String propertyName, Object value) {
        SimpleExpression se = new SimpleExpression(tableName, propertyName, value, "!=");
        return se;
    }

    public static Criteria like(String tableName, String propertyName, Object value) {
        SimpleExpression se = new SimpleExpression(tableName, propertyName, value, "LIKE");
        return se;
    }

    public static Criteria gt(String tableName, String propertyName, Object value) {
        SimpleExpression se = new SimpleExpression(tableName, propertyName, value, ">");
        return se;
    }

    public static Criteria lt(String tableName, String propertyName, Object value) {
        SimpleExpression se = new SimpleExpression(tableName, propertyName, value, "<");
        return se;
    }

    public static Criteria le(String tableName, String propertyName, Object value) {
        SimpleExpression se = new SimpleExpression(tableName, propertyName, value, "<=");
        return se;
    }

    public static Criteria ge(String tableName, String propertyName, Object value) {
        SimpleExpression se = new SimpleExpression(tableName, propertyName, value, ">=");
        return se;
    }

    public static Criteria between(String tableName, String propertyName, Object min, Object max) {
        IntervalExpression ie = new IntervalExpression(tableName, propertyName, min, max);
        return ie;
    }
}
