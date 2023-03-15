package com.cmbc.infras.dto.event;

import lombok.Data;

import java.io.Serializable;

@Data
public class HistoryAlarmParam implements Serializable {
    private static final long serialVersionUID = -372652185863730023L;

    /**
     * 内容,位置,监控对象-搜索关键字
     */
    private String content;

    /**
     * 搜索的起止时间
     * long型时间戳
     */
    private Long beginTime;
    private Long endTime;

    /**
     * 分页参数
     */
    //页码
    private int number;
    //页大小pageSize
    private int size;
    //总条数
    private int total;

    private String bankId;

    //告警状态-21.11.16加
    private String eventLevel;

}
