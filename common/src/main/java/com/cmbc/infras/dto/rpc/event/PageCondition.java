package com.cmbc.infras.dto.rpc.event;

import lombok.Data;

@Data
public class PageCondition {
    private String number;
    private int size;

    public PageCondition() { }

    public PageCondition(String number, int size) {
        this.number = number;
        this.size = size;
    }
}
