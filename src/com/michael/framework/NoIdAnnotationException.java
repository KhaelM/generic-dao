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
public class NoIdAnnotationException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public NoIdAnnotationException(Class<?> clazz) {
        super("La classe " + clazz.getName() + " ne contient pas l'annotation @Id");
    }
}
