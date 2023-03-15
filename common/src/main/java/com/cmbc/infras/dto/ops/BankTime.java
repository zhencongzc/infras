package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BankTime implements Serializable {
    private static final long serialVersionUID = -5436951600001340131L;

    private int id;
    private String bankId;
    private String bankName;
    private Date runTime;

    public BankTime() {}

}
