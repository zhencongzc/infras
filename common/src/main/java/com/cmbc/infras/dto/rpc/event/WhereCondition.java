package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WhereCondition {
    private List<AndCondition> and = new ArrayList<>();

    public WhereCondition(List<QueryCondition> conditions) {
        and.add(new AndCondition(conditions));
    }

    public WhereCondition(List<QueryCondition> conditions, List<QueryCondition> ors) {
        and.add(new AndCondition(conditions, ors));
    }

}