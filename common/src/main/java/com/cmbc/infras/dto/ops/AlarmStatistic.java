package com.cmbc.infras.dto.ops;

import com.cmbc.infras.constant.AlarmStatisEnum;
import com.cmbc.infras.dto.event.AlarmCount;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 总行运维界面-银行信息
 * [名称,紧急,严重,重要,总]
 */
@Data
public class AlarmStatistic implements Serializable {
    //See-AlarmStatisEnum
    private int type;
    //主机房,二级分行,支行,村镇银行
    private String groupName;
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

    public AlarmStatistic() { }

    public AlarmStatistic(int urgency, int urgencyDone, int serious, int seriousDone, int important, int importantDone, int total, int totalDone) {
        this.urgency = urgency;
        this.urgencyDone = urgencyDone;
        this.serious = serious;
        this.seriousDone = seriousDone;
        this.important = important;
        this.importantDone = importantDone;
        this.total = total;
        this.totalDone = totalDone;
    }

    /**
     * 计算统计数
     */
    public AlarmStatistic(AlarmStatisEnum e, AlarmCount count, AlarmCount countDone){
        this(e, count, countDone, null);
    }

    /**
     * statistic非空时
     * 上级统计数据减去下级统计数据
     * 支行数量值需 - 村镇数量值
     */
    public AlarmStatistic(AlarmStatisEnum e, AlarmCount count, AlarmCount countDone, AlarmStatistic statistic) {
        if (count == null || countDone == null) {
            this.type = e.getCode();
            this.groupName = e.getDesc();
            return;
        }
        Map<String, Integer> cmap = count.getGroup();
        Map<String, Integer> dmap = countDone.getGroup();
        if (cmap == null || dmap == null) {
            this.type = e.getCode();
            this.groupName = e.getDesc();
            return;
        }

        this.urgency = cmap.get("1") == null ? 0 : cmap.get("1");
        this.serious = cmap.get("2") == null ? 0 : cmap.get("2");
        this.important = cmap.get("3") == null ? 0 : cmap.get("3");
        this.total = count.getCount();

        this.urgencyDone = dmap.get("1") == null ? 0 : dmap.get("1");
        this.seriousDone = dmap.get("2") == null ? 0 : dmap.get("2");
        this.importantDone = dmap.get("3") == null ? 0 : dmap.get("3");
        this.totalDone = countDone.getCount();

        /**
         * 没用到
         */
        if (statistic != null) {
            this.urgency = this.urgency - statistic.urgency;
            this.serious = this.serious - statistic.serious;
            this.important = this.important - statistic.important;

            this.urgencyDone = this.urgencyDone - statistic.urgencyDone;
            this.seriousDone = this.seriousDone - statistic.seriousDone;
            this.importantDone = this.importantDone - statistic.importantDone;
        }

        this.type = e.getCode();
        this.groupName = e.getDesc();
    }

    public void set(AlarmStatisEnum en) {
        this.type = en.getCode();
        this.groupName = en.getDesc();
    }

}
