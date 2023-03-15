package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

/**
 * 总行主界面-告警雷达
 * 总行主界面-告警汇总-轮播地图-某一级分行
 */
@Data
public class AlarmRadar implements Serializable {
    private static final long serialVersionUID = 8294879042848051916L;

    //银行ID-NAME
    private String bankId;
    private String bankName;
    //分行告警(处理&受理)-本机房
    private int branch;
    private int branchDone;
    //二级分行
    private int branch2;
    private int branch2Done;
    //支行
    private int sub;
    private int subDone;
    //村镇银行
    private int town;
    private int townDone;
    //总
    private int total;
    private int totalDone;

    /**
     * 当前状态
     * 有告警则为异常
     * 0:正常,1:异常
     */
    private int status = 0;
    private String statusName = "正常";

    public AlarmRadar sum() {
        total = branch + branch2 + sub + town;
        totalDone = branchDone + branch2Done + subDone + townDone;
        if ((branch - branchDone) > 0) {
            this.status = 1;
        } else if ((branch2 - branch2Done) > 0) {
            this.status = 1;
        } else if ((sub - subDone) > 0) {
            this.status = 1;
        } else if ((town - townDone) > 0) {
            this.status = 1;
        }
        if (this.status == 1) {
            this.statusName = "异常";
        }
        return this;
    }
}
