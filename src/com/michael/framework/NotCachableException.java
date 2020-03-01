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
public class NotCachableException extends RuntimeException {

    public NotCachableException(Class clazz) {
        super("La classe " + clazz.getName() + " ne possède pas l'annotation @Cache.");
    }
    
}
