package com.cmbc.infras.dto.ops;

import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.event.CountDoneResult;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 分行主界面-告警雷达
 * [分行名称,紧急,严重,重要,总,当前状态]
 */
@Data
public class BranchRadar implements Serializable {

    private static final long serialVersionUID = -6429255216679097586L;

    private String bankId;
    private String bankName;
    //紧急
    private int urgency;
    private int urgencyDone;
    //严重
    private int serious;
    private int seriousDone;
    //重要
    private int important;
    private int importantDone;
    //总
    private int total;
    private int totalDone;
    //当前状态，有告警则为异常，0:正常,1:异常
    private int status = 0;
    private String statusName = "正常";

    public BranchRadar(Bank bank, CountDoneResult data) {
        Map<String, Integer> cmap = data.getCount().getGroup();
        Map<String, Integer> dmap = data.getCountDone().getGroup();
        this.urgency = cmap.get("1") == null ? 0 : cmap.get("1");
        this.serious = cmap.get("2") == null ? 0 : cmap.get("2");
        this.important = cmap.get("3") == null ? 0 : cmap.get("3");
        this.total = data.getCount().getCount();
        this.urgencyDone = dmap.get("1") == null ? 0 : dmap.get("1");
        this.seriousDone = dmap.get("2") == null ? 0 : dmap.get("2");
        this.importantDone = dmap.get("3") == null ? 0 : dmap.get("3");
        this.totalDone = data.getCountDone().getCount();
        this.bankId = bank.getBankId();
        this.bankName = bank.getBankName();
        if ((urgency - urgencyDone) > 0) this.status = 1;
        if ((serious - seriousDone) > 0) this.status = 1;
        if ((important - importantDone) > 0) this.status = 1;
        if ((urgency - urgencyDone) > 0) this.status = 1;
        if (this.status == 1) this.statusName = "异常";
    }

}
