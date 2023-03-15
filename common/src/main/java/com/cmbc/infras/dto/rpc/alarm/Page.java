package com.cmbc.infras.dto.rpc.alarm;


import lombok.Data;

import java.io.Serializable;

@Data
public class Page implements Serializable {
    private static final long serialVersionUID = 2535959856491202141L;

    private int number; //1
    private int size;   //20

    public Page() {}

    public Page(int number, int size) {
        this.number = number;
        this.size = size;
    }

}
