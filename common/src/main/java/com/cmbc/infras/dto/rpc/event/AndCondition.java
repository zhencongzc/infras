package com.cmbc.infras.dto.rpc.event;


import lombok.Data;

import java.util.List;

@Data
public class AndCondition {
    private List<QueryCondition> and;
    private List<QueryCondition> or;

    public AndCondition(List<QueryCondition> and) {
        this.and = and;
    }

    public AndCondition(List<QueryCondition> and, List<QueryCondition> or) {
        this.and = and;
        this.or = or;
    }

}