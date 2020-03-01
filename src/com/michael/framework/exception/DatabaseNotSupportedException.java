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
public class DatabaseNotSupportedException extends RuntimeException {

    public DatabaseNotSupportedException(String baseDeDonnees) {
        super("La base de donnée " + baseDeDonnees + " n'est pas supportée.");
    }
    
}
