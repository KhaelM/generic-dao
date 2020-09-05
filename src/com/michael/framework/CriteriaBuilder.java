/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.michael.framework;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author michael
 */
public class CriteriaBuilder {

    private List<Criteria> criterias;
    private List<String> logicalOperators;
    private Class<?> clazz;
    private static Pagination pagination;

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(int page, int nombreResultat) {
        if (CriteriaBuilder.pagination == null) {
            CriteriaBuilder.pagination = new Pagination(page, nombreResultat);
        } else {
            CriteriaBuilder.pagination.setNombreResultat(nombreResultat);
            CriteriaBuilder.pagination.setPage(page);
        }
    }

    public CriteriaBuilder(Class<?> clazz) {
        this.clazz = clazz;
        criterias = new ArrayList<Criteria>();
        logicalOperators = new ArrayList<String>();
    }

    public void add(Criteria criteria, String endLogicalOperator) {
        logicalOperators.add(endLogicalOperator);
        criterias.add(criteria);
    }

    public List<Criteria> getCriterias() {
        return criterias;
    }

    public List<String> getLogicalOperators() {
        return logicalOperators;
    }

    public Class<?> getClazz() {
        return clazz;
    }
}
