package com.cmbc.infras.dto.rpc.alarm;

import lombok.Data;

import java.io.Serializable;

@Data
public class SortItem implements Serializable {
    private static final long serialVersionUID = -5298265314644047943L;

    private String field;   //"event_time"
    private String type;    //"DESC"

    public SortItem() {}

    public SortItem(String field, String type) {
        this.field = field;
        this.type = type;
    }

}
