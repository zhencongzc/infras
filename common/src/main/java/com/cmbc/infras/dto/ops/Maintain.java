package com.cmbc.infras.dto.ops;

import lombok.Data;

import java.io.Serializable;

@Data
public class Maintain implements Serializable {
    private static final long serialVersionUID = 6164640345454222439L;

    //作业复核
    private String workCheck;
    //巡检复核进度
    private String patrolCheck;
    //实时得分
    private String currentScore;
    //年平均分
    private String averageScore;

    public Maintain(String work, String partol, String scope, String average) {
        this.workCheck = work;
        this.patrolCheck = partol;
        this.currentScore = scope;
        this.averageScore = average;
    }

}
