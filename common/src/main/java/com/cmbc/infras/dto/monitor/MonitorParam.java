package com.cmbc.infras.dto.monitor;

import lombok.Data;

import java.io.Serializable;

/**
 * 监控数据查询参数
 */
@Data
public class MonitorParam implements Serializable {
    private static final long serialVersionUID = 733933647880844194L;

    //用户账号
    private String account;
    /**
     * 设备类型10:UPS,20:AIR,30:Hum
     * @See DeviceTypeEnum
     */
    private Integer deviceType;
    /**
     * 测点类型
     * @See SpotTypeEnum
     */
    private Integer spotType;

    private String bankId;

    public MonitorParam() {}

    public MonitorParam(String account) {
        this.account = account;
    }

    public MonitorParam(String account, Integer deviceType) {
        this.account = account;
        this.deviceType = deviceType;
    }

    public MonitorParam(String account, String bankId, Integer deviceType) {
        this.account = account;
        this.bankId = bankId;
        this.deviceType = deviceType;
    }

}
