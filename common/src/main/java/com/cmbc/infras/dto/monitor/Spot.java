package com.cmbc.infras.dto.monitor;

import lombok.Data;

import java.io.Serializable;

@Data
public class Spot implements Serializable {
    private static final long serialVersionUID = 3800491259159940639L;

    private String bankId;
    private String bankName;
    private String contact;
    //private int deviceType;

    private int id;
    private String deviceId;
    private String deviceName;

    private String spotId;
    private String spotName;
    private int spotType;

}
