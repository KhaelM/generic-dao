/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.michael.framework.exception.designPattern;

/**
 *
 * @author michael
 */
public class SingletonException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public SingletonException(String message) {
        super(message);
    }
}
