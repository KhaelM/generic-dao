/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.michael.framework;

import java.lang.reflect.Field;

/**
 *
 * @author michael
 */
public class InfoForLeftJoin {
    Class clazz;
    Object instance;
    Object instanceConteneur;
    Field fieldConteneur;

    public InfoForLeftJoin(Class clazz, Object instance, Object instanceConteneur, Field fieldConteneur) {
        this.clazz = clazz;
        this.instance = instance;
        this.instanceConteneur = instanceConteneur;
        this.fieldConteneur = fieldConteneur;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Object getInstance() {
        return instance;
    }

    public void setInstance(Object instance) {
        this.instance = instance;
    }

    public Object getInstanceConteneur() {
        return instanceConteneur;
    }

    public void setInstanceConteneur(Object instanceConteneur) {
        this.instanceConteneur = instanceConteneur;
    }

    public Field getFieldConteneur() {
        return fieldConteneur;
    }

    public void setFieldConteneur(Field fieldConteneur) {
        this.fieldConteneur = fieldConteneur;
    }
}
