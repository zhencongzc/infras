package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

@Data
public class Appraise implements Serializable {
    private static final long serialVersionUID = 8873121499895309151L;

    private String areaId;
    private String areaName;

    //系统架构
    private String system;
    //设备运行
    private String operation;
    //运维管理
    private String ops;

    public Appraise() {}

    public Appraise(String areaName, String system, String operation, String ops) {
        this.areaName = areaName;
        this.system = system;
        this.operation = operation;
        this.ops = ops;
    }

}
