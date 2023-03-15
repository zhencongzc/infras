package com.cmbc.infras.health.dto;

import lombok.Data;

import java.util.List;

/**
 * KE-报表报告-数据查询-参数查询的请求参数
 */
@Data
public class ReportRequestParam {

    private long start;//开始日期
    private long end;//结束日期
    private List<String> ids;//测点集合
    private String agg_interval = "day";//按天统计
    private String[] aggregator = new String[]{"avg"};//统计平均值

    public ReportRequestParam(long start, long end, List<String> ids) {
        this.start = start;
        this.end = end;
        this.ids = ids;
    }
}
