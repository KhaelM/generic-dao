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
public class Pagination {
    private int page;
    private int nombreResultat;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getNombreResultat() {
        return nombreResultat;
    }

    public void setNombreResultat(int nombreResultat) {
        this.nombreResultat = nombreResultat;
    }
    
    public Pagination(int page, int nombreResultat) {
        this.page = page;
        this.nombreResultat = nombreResultat;
    }
    
}
