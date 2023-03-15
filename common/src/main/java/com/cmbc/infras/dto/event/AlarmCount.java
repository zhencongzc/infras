package com.cmbc.infras.dto.event;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class AlarmCount implements Serializable {
    private static final long serialVersionUID = -2219046977450565203L;

    private int count;
    private Map<String, Integer> group;
}
