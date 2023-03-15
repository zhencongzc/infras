package com.cmbc.infras.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class LabelParam implements Serializable {
    private static final long serialVersionUID = 8444432598234888694L;

    /**List参数**/
    //告警等级->eventLevel
    private List<String> eventLevel;
    //处理状态->processState
    private List<String> status;
    //恢复状态->recoverState
    private List<String> recoverStatus;

    //库表ID
    private int id;
    //标签名称
    private String name;
    //位置-银行名称-匹配告警位置项
    private String eventLocation;

}
