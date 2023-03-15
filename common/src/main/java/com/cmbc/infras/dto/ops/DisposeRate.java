package com.cmbc.infras.dto.ops;

import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.util.NumberUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class DisposeRate implements Serializable {
    private static final long serialVersionUID = 3248885381459385347L;

    //未处理0
    private String not;
    //处理中1
    private String being;
    //已处理2
    private String already;

    public DisposeRate() {
    }

    public DisposeRate(String not, String being, String already) {
        this.not = not;
        this.being = being;
        this.already = already;
    }

    public DisposeRate(int un, int ing, int already, int total) {
        this.not = NumberUtils.getPersent(un, total);
        this.being = NumberUtils.getPersent(ing, total);
        this.already = NumberUtils.getPersent(already, total);
    }

    public DisposeRate(HashMap<String, Integer> alarmCount) {
        int not = alarmCount.get("not");
        int being = alarmCount.get("being");
        int already = alarmCount.get("already");
        int total = not + being + already;
        if (total == 0) {
            this.not = "0%";
            this.being = "0%";
            this.already = "100%";
        } else {
            this.not = NumberUtils.getPersent(not, total);
            this.being = NumberUtils.getPersent(being, total);
            this.already = NumberUtils.getPersent(already, total);
        }
    }
}
