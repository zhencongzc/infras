package com.cmbc.infras.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 三方页面接口
 * 标签
 */
@Data
public class Label implements Serializable {
    private static final long serialVersionUID = 1987228831516701678L;

    //库表ID
    private int id;
    //2021-12-13新增account-与账号关联
    private String account;
    //标签名称
    private String labelName;
    //告警等级
    private String eventLevel;
    //处理状态
    private String processState;
    //恢复状态
    private String recoverState;
    //位置-银行名称-匹配告警位置项
    private String location;
    //是否选中0:未选中,1:选中
    private int checked;

    public Label() {}

    public Label(String labelName, String eventLevel) {
        this.labelName = labelName;
        this.eventLevel = eventLevel;
    }
}
