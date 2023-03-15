package com.cmbc.infras.dto.monitor;

import com.cmbc.infras.constant.AirStateEnum;
import com.cmbc.infras.dto.Device;
import lombok.Data;

@Data
public class AirInfo extends Device {

    private String backTemper;//回风温度
    private String backHumidity;//回风湿度
    private int state;//状态

    public AirInfo(Device dev) {
        super(dev);
    }

    public String getStateName() {
        return AirStateEnum.getDesc(state);
    }

}
