package com.cmbc.infras.dto.monitor;

import com.cmbc.infras.dto.Device;
import lombok.Data;

@Data
public class UpsInfo extends Device {

    private String power; //功率
    private String loadRate;//负载
    private String backTime;//电池后备时间
    private int alarm = 0;//1时红色显示

    public UpsInfo(Device dev) {
        super(dev);
    }

}
