package com.cmbc.infras.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 银行pue指标-视图实体
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BankKpiVo implements Serializable {
    /**
     * 银行ID
     */
    private String bankId;
    /**
     * 银行名称
     */
    private String bankName;
    /**
     * pueDeviceId
     */
    private String pueDeviceId;
    /**
     * pueSpotId
     */
    private String pueSpotId;
    /**
     * KE系统工程组态获取实时pue测点值
     */
    private String pue;
    /**
     * 紧急告警响应率
     */
    private String rate;
    /**
     * state:1和上一个周期比,涨了,state:0和上一个周期比,降了
     */
    private String state;
}
