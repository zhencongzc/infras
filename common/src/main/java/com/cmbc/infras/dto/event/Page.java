package com.cmbc.infras.dto.event;

import lombok.Data;

@Data
public class Page {
    private int total;
    private int number;
    private int size;
}
