package com.cmbc.infras.dto.event;

import lombok.Data;

import java.io.Serializable;

@Data
public class CountDoneResult implements Serializable {
    private static final long serialVersionUID = 3563525418704919745L;

    private AlarmCount count;
    private AlarmCount countDone;

    public CountDoneResult() { }

    public CountDoneResult(AlarmCount count, AlarmCount countDone) {
        this.count = count;
        this.countDone = countDone;
    }
}
