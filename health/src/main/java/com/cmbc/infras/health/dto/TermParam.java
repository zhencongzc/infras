package com.cmbc.infras.health.dto;

import lombok.Data;

/**
 * 用于构建“告警信息”请求参数中的条件
 */
@Data
public class TermParam {

    private String field;
    private String operator;
    private Object value;

    public TermParam() {
    }

    public TermParam(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

}

