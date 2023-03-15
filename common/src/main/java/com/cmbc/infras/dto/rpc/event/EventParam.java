package com.cmbc.infras.dto.rpc.event;

import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.util.DateTimeUtils;
import com.cmbc.infras.util.Utils;
import lombok.Data;

import java.util.*;

@Data
public class EventParam {

    private WhereCondition where;

    private List<SortCondition> sorts;

    private PageCondition page;

    //private String group;

    private boolean extra = true;

    public EventParam(List<QueryCondition> list, List<SortCondition> sorts, PageCondition page) {
        this.where = new WhereCondition(list);
        this.sorts = sorts;
        this.page = page;
    }

    /**
     * 默认查询条件
     * last:是否实时告警,default true
     */
    public EventParam() {
        this(true);
    }

    public EventParam(boolean last) {
        List<QueryCondition> list = new ArrayList<>();
        list.add(new QueryCondition("is_confirm","eq",0));
        list.add(new QueryCondition("cep_processed", "eq", 0));
        list.add(new QueryCondition("event_level", "in", Arrays.asList("1", "2", "3", "4", "5")));
        list.add(new QueryCondition("is_accept", "in", Arrays.asList("0", "1", "2")));
        list.add(new QueryCondition("is_recover", "in", Arrays.asList("0", "1")));
        //非实时告警
        if (!last) {
            list.add(new QueryCondition("event_time", "gte", DateTimeUtils.getTodayZeroDot()));
            list.add(new QueryCondition("event_time", "lte", System.currentTimeMillis()));
        }
        WhereCondition where = new WhereCondition(list);

        List<SortCondition> sorts = new ArrayList(){{add(new SortCondition("event_time", "DESC"));}};
        PageCondition page = new PageCondition("1", InfrasConstant.ALARM_PAGE_SIZE);

        this.where = where;
        this.sorts = sorts;
        this.page = page;
    }

    /*
    public static Map<String, Object> createDefaultParam() {
        return EventParam.createDefaultParam(true);
    }

    public static Map<String, Object> createDefaultParam(boolean last) {
        Map<String, Object> map = new HashMap<>();
        List<QueryCondition> list = new ArrayList<>();
        list.add(new QueryCondition("is_confirm","eq",0));
        list.add(new QueryCondition("cep_processed", "eq", 0));
        list.add(new QueryCondition("event_level", "in", Arrays.asList("1", "2", "3", "4", "5")));
        list.add(new QueryCondition("is_accept", "in", Arrays.asList("0", "1", "2")));
        list.add(new QueryCondition("is_recover", "in", Arrays.asList("0", "1")));
        //非实时告警
        if (!last) {
            list.add(new QueryCondition("event_time", "gte", DateTimeUtils.getTodayZeroDot()));
            list.add(new QueryCondition("event_time", "lte", System.currentTimeMillis()));
        }
        WhereCondition where = new WhereCondition(list);

        List<SortCondition> sorts = new ArrayList(){{add(new SortCondition("event_time", "DESC"));}};
        PageCondition page = new PageCondition("1", InfrasConstant.ALARM_PAGE_SIZE);

        map.put("where", where);
        map.put("sorts", sorts);
        map.put("page", page);
        return map;
    }*/
}






