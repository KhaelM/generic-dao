/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.michael.framework.exception;

/**
 *
 * @author michael
 */
public class JdbcConfigurationException extends RuntimeException {
    public JdbcConfigurationException(String message) {
        super(message);
    }

    public JdbcConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
