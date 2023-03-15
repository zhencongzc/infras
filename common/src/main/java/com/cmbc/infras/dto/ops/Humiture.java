package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

/**
 * 机房温湿度
 * 分行名称,最低温,最高温,最低湿,最高湿
 */
@Data
public class Humiture implements Serializable {
    private static final long serialVersionUID = -2514166484984398805L;
    //分行ID,分行名称
    private String bankId;
    private String bankName;
    //最低最高温度
    private String minTemper;
    private String maxTemper;
    //最低最高湿度
    private String minHumidity;
    private String maxHumidity;

    public Humiture initEmpty() {
        this.minHumidity = "--";
        this.maxHumidity = "--";
        this.minTemper = "--";
        this.maxTemper = "--";
        return this;
    }

}
