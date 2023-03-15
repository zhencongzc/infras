package com.cmbc.infras.dto.ops;

import com.cmbc.infras.dto.monitor.Humidity;
import com.cmbc.infras.dto.monitor.UpsInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分行运维信息界面
 * 二级分行支行村镇
 * UPS信息,温湿度,空调告警
 */
@Data
public class OpsBankInfo implements Serializable {
    private static final long serialVersionUID = 7257033328847057124L;

    /**
     * UPS信息:名,功率,负载,后备时间
     * 温湿度:温度,湿度
     * 空调告警则显示 空调(红色)
     */

    private List<UpsInfo> upss;

    private Humidity humidity;
    /**
     * airAlarm=0,不告警
     * airAlarm=1,告警
     */
    private int airAlarm = 0;

}
