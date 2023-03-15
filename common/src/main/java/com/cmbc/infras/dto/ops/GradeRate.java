package com.cmbc.infras.dto.ops;

import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.util.NumberUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class GradeRate implements Serializable {
    private static final long serialVersionUID = 534789847882296735L;
    //--EventLevelEnum--
    //紧急1
    private String urgency;
    //严重2
    private String serious;
    //重要3
    private String important;
    //次要的4
    private String secondary;
    //预警5
    private String warning;

    public GradeRate() {}

    public GradeRate(int urg, int ser, int imp, int sec, int war, int total) {
        this.urgency = NumberUtils.getPersent(urg, total);
        this.serious = NumberUtils.getPersent(ser, total);
        this.important = NumberUtils.getPersent(imp, total);
        this.secondary = NumberUtils.getPersent(sec, total);
        this.warning = NumberUtils.getPersent(war, total);
    }

    public GradeRate(int i) {
        if (i == 0) {
            this.urgency = "0%";
            this.serious = "0%";
            this.important = "0%";
            this.secondary = "0%";
            this.warning = "0%";
        }
    }

    public GradeRate(AlarmCount alarmCount) {
        int total = alarmCount.getCount();
        if (total == 0) {
            this.urgency = "0%";
            this.serious = "0%";
            this.important = "0%";
            this.secondary = "0%";
            this.warning = "0%";
            return;
        }
        Map<String, Integer> group = alarmCount.getGroup();
        Integer urgency = group.get("1") == null ? 0 : group.get("1");
        Integer serious = group.get("2") == null ? 0 : group.get("2");
        Integer important = group.get("3") == null ? 0 : group.get("3");
        Integer secondary = group.get("4") == null ? 0 : group.get("4");
        Integer warning = group.get("5") == null ? 0 : group.get("5");
        this.urgency = NumberUtils.getPersent(urgency, total);
        this.serious = NumberUtils.getPersent(serious, total);
        this.important = NumberUtils.getPersent(important, total);
        this.secondary = NumberUtils.getPersent(secondary, total);
        this.warning = NumberUtils.getPersent(warning, total);
    }
}
