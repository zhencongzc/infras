package com.cmbc.infras.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Bank implements Serializable {
    private static final long serialVersionUID = 4707403502079998805L;

    private int id;//自增ID
    private String bankId;//银行ID-resource_id
    private String bankName;//银行名
    private String parentId;//父节点id
    private String contactId;//联系人1
    private String contact;//联系人2
    private int sort;//排序
    private int level;//层级[0总行,1分行,2二级分行,3支行,4村镇]
    private double lng;//经度longitude
    private double lat;//纬度latitude
    private String areaId;//区域ID
    private String areaName;//区域名
    private String remark;//备注
    private String linkId;//连接视图中的id
    private String pue;//前台显示PUE值

    private List<Bank> subs;
    private List<Device> devices;
    private List<String> spotIds;

    public Bank() {
    }

    public Bank(String bankId) {
        this.bankId = bankId;
    }

    public Bank(String bankId, String bankName) {
        this.bankId = bankId;
        this.bankName = bankName;
    }


}
