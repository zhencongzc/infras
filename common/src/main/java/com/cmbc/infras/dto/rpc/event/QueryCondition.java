package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

@Data
public class QueryCondition {
    private String field;
    private String operator;
    private Object value;

    public QueryCondition() { }

    public QueryCondition(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }
}
