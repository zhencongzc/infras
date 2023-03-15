package com.cmbc.infras.dto.ops;

import com.cmbc.infras.dto.Device;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Asset implements Serializable {
    private static final long serialVersionUID = 8668294182050818284L;

    //负载-UPS
    private String upsLoad;
    //PUE
    private String pue;
    //柴发后备时间
    private String dynamoTime;
    //电池后备时间-UPS
    private String backTime;

    private List<Device> upss = new ArrayList<>();
    private Device pueDev;
    private Device chaifa;


    public Asset() {
    }

    /**
     * 数据空时构造方法
     */
    public Asset(boolean test) {
        if (test) {
            this.upsLoad = "-";
            this.pue = "-";
            this.dynamoTime = "-";
            this.backTime = "-";
        }
    }

    public Asset deal() {
        this.upss = null;
        this.pueDev = null;
        this.chaifa = null;
        if (this.dynamoTime == null) this.dynamoTime = "0";
        return this;
    }
}
