package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

@Data
public class Energy implements Serializable {
    private static final long serialVersionUID = 7218635504847036801L;

    //PUE值
    private String pue;
    //总功率
    private String power;
    //IT功率
    private String itPower;
    //时间HH:mm:ss
    private String time;

}
