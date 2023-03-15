package com.cmbc.infras.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 告警指标-视图实体
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AlarmKpiVo implements Serializable {
    /**
     * 指标名称
     */
    private String kpiName;
    /**
     * 指标值
     */
    private String kpiValue;
    /**
     * 指标趋势
     */
    private String kpiTrend;
    /**
     * 指标趋势颜色 0 为降 1为升
     */
    private String kpiTrendState;
}
