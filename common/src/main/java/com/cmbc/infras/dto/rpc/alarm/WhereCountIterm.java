package com.cmbc.infras.dto.rpc.alarm;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史告警-查count
 * where,group
 */
@Data
public class WhereCountIterm implements Serializable {
    private static final long serialVersionUID = -6011303805311411885L;

    private List<TermItem> where;
    private String group;

    public WhereCountIterm() {}

    public WhereCountIterm(List<TermItem> where) {
        this.where = where;
    }

    public WhereCountIterm add(TermItem item) {
        if (where == null) {
            where = new ArrayList<>();
        }
        this.where.add(item);
        return this;
    }

    public WhereCountIterm configGroup(String group) {
        this.group = group;
        return this;
    }
}
