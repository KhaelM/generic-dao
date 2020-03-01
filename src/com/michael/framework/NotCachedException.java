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
public class NotCachedException extends Exception {
    public NotCachedException(Class clazz) {
        super("La classe " + clazz + " n'as pas été enregistré dans le cache.");
    }
}
