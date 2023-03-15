package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

@Data
public class SortCondition {
    private String field;
    private String type;

    public SortCondition() { }

    public SortCondition(String field, String type) {
        this.field = field;
        this.type = type;
    }
}
