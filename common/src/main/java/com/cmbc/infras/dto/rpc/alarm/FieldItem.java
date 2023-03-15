package com.cmbc.infras.dto.rpc.alarm;

import lombok.Data;

import java.io.Serializable;

@Data
public class FieldItem implements Serializable {
    private static final long serialVersionUID = 4306520794363528907L;

    private String field;
    private String operator;
    private Object value;

    public FieldItem() {}

    public FieldItem(String field, String operator) {
        this.field = field;
        this.operator = operator;
    }

    public FieldItem(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

}

