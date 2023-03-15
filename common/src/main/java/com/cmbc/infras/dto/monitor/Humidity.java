package com.cmbc.infras.dto.monitor;

import com.cmbc.infras.constant.AirStateEnum;
import com.cmbc.infras.dto.Device;
import lombok.Data;

@Data
public class Humidity extends Device {

    private String temper;

    private String humidity;

    private int state;

    public Humidity() {}

    public Humidity(Device dev) {
        super(dev);
    }

    public String getStateName() {
        return AirStateEnum.getDesc(state);
    }

}
