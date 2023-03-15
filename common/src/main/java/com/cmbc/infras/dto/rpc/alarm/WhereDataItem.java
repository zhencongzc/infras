package com.cmbc.infras.dto.rpc.alarm;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史告警-查data
 * where,page,sorts--无group
 */
@Data
public class WhereDataItem implements Serializable {
    private static final long serialVersionUID = -5815630806851148098L;

    private List<TermItem> where;
    private List<SortItem> sorts;
    private Page page;

    public WhereDataItem() {}

    public WhereDataItem(List<TermItem> where) {
        this.where = where;
    }

    public void setPage(int number, int size) {
        Page page = new Page(number, size);
        this.page = page;
    }

    public void setSort(String field, String type) {
        List<SortItem> sorts = new ArrayList<>();
        SortItem sort = new SortItem(field, type);
        sorts.add(sort);
        this.sorts = sorts;
    }

}

