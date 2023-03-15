package com.cmbc.infras.dto;

import com.cmbc.infras.dto.monitor.Spot;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class Device implements Serializable {
    private static final long serialVersionUID = -9155990340289443086L;

    private int id;
    private String bankId;
    private String bankName;
    private int bankLevel;
    private String contact;
    private String deviceId;
    private String keName;
    private String deviceName;
    private int deviceType;
    private String groupName;
    private String trueDeviceId;
    private List<Spot> spots = new ArrayList<>();

    public Device() {
    }

    public Device(Device dev) {
        this.id = dev.id;
        this.bankId = dev.bankId;
        this.bankName = dev.bankName;
        this.bankLevel = dev.bankLevel;
        this.contact = dev.contact;
        this.deviceId = dev.deviceId;
        this.keName = dev.keName;
        this.deviceName = dev.deviceName;
        this.deviceType = dev.deviceType;
        this.groupName = dev.groupName;
        this.trueDeviceId = dev.trueDeviceId;
    }
}
