package com.cmbc.infras.util;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.HistoryAlarmParam;
import com.cmbc.infras.dto.rpc.event.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 实时告警参数工具类
 * 历史告警参数类AlarmParamUtils
 */
public class EventParamUtils {

    /**
     * 实时告警参数创建
     */
    public static EventGroupParam creatLastAlarmParam() {
        return creatLastAlarmParam(null);
    }

    public static EventGroupParam creatLastAlarmParam(Label label) {
        EventParam eventParam = createEventParam(label);
        EventGroupParam param = new EventGroupParam(eventParam);
        return param;
    }

    public static EventParam createEventParam() {
        return createEventParam(null);
    }

    public static EventParam createEventParam(Label label) {
        if (label == null) {
            //不传参使用默认参数-前台告警展示 1:紧急,2:严重的
            label = new Label("defaultLabel", "1,2");
        }
        EventParam param = new EventParam();
        List<QueryCondition> list = new ArrayList<>();
        list.add(new QueryCondition("is_confirm", "eq", 0));
        list.add(new QueryCondition("cep_processed", "eq", 0));

        if (StringUtils.isNotBlank(label.getEventLevel())) {
            list.add(new QueryCondition("event_level", "in", label.getEventLevel().split(",")));
        } else {
            list.add(new QueryCondition("event_level", "in", new String[]{"1", "2", "3", "4", "5"}));
        }

        if (StringUtils.isNotBlank(label.getProcessState())) {
            list.add(new QueryCondition("is_accept", "in", label.getProcessState().split(",")));
        } else {
            list.add(new QueryCondition("is_accept", "in", new String[]{"0", "1", "2"}));
        }

        if (StringUtils.isNotBlank(label.getRecoverState())) {
            list.add(new QueryCondition("is_recover", "in", label.getRecoverState().split(",")));
        } else {
            list.add(new QueryCondition("is_recover", "in", new String[]{"0", "1"}));
        }

        WhereCondition where = new WhereCondition(list);

        List<SortCondition> sorts = new ArrayList() {{
            add(new SortCondition("event_time", "DESC"));
        }};
        PageCondition page = new PageCondition("1", InfrasConstant.ALARM_PAGE_SIZE);

        param.setWhere(where);
        param.setSorts(sorts);
        param.setPage(page);
        param.setExtra(true);
        return param;
    }

    /**
     * 创建历史告警参数
     */
    public static EventParam createHistoryParam(HistoryAlarmParam haParam) {
        List<QueryCondition> list = new ArrayList<>();
        List<QueryCondition> or = new ArrayList<>();

        //开始时间范围
        if (haParam.getBeginTime() == null) {
            list.add(new QueryCondition("event_time", "gte", DateTimeUtils.getCurrentMonthDot()));
        } else {
            list.add(new QueryCondition("event_time", "gte", haParam.getBeginTime()));
        }
        //结速时间范围
        if (haParam.getEndTime() == null) {
            list.add(new QueryCondition("event_time", "lte", DateTimeUtils.getCurrentTime()));
        } else {
            list.add(new QueryCondition("event_time", "lte", haParam.getEndTime()));
        }

        //只查告警(notin 7) 查事件(eq 7) --这里只查历史告警实时告警
        list.add(new QueryCondition("event_type", "notin", 7));
        //默认条件--
        list.add(new QueryCondition("masked", "eq", 0));
        list.add(new QueryCondition("cep_processed", "eq", 0));

        //默认等级
        if (StringUtils.isBlank(haParam.getEventLevel())) {
            list.add(new QueryCondition("event_level", "in", Arrays.asList("1", "2", "3", "4", "5")));
        } else {
            list.add(new QueryCondition("event_level", "in", haParam.getEventLevel().split(",")));
        }

        //搜索文本项
        if (StringUtils.isNotBlank(haParam.getContent())) {
            StringBuffer sb = new StringBuffer("%").append(haParam.getContent()).append("%");
            or.add(new QueryCondition("content", "like", sb.toString()));
            or.add(new QueryCondition("event_source", "like", sb.toString()));
        }

        WhereCondition where = null;
        if (or.isEmpty()) {
            where = new WhereCondition(list);
        } else {
            where = new WhereCondition(list, or);
        }

        List<SortCondition> sorts = new ArrayList() {{
            add(new SortCondition("event_time", "DESC"));
        }};

        //分页查询参数
        PageCondition page = null;
        if (haParam.getNumber() == 0 || haParam.getSize() == 0) {
            page = new PageCondition("1", 20);
        } else {
            page = new PageCondition(haParam.getNumber() + "", haParam.getSize());
        }

        EventParam param = new EventParam();
        param.setWhere(where);
        param.setSorts(sorts);
        param.setPage(page);
        return param;
    }

    /**
     * Event Result
     ***/

    public static List<Event> parseEventResult(String str) {
        JSONObject obj = JSONObject.parseObject(str);
        String code = obj.getString("error_code");
        Assert.state("00".equals(code), "查询告警数据出错！");
        String data = obj.getString("data");
        EventResult eventResult = JSON.parseObject(data, EventResult.class);
        return eventResult.getEvent_list();
    }

    public static AlarmCount parseCountResult(String countStr) {
        JSONObject countObj = JSONObject.parseObject(countStr);
        String countCode = countObj.getString("error_code");
        Assert.state("00".equals(countCode), "查询告警数量出错！");
        String countData = countObj.getString("data");
        AlarmCount alarmCount = JSON.parseObject(countData, AlarmCount.class);
        return alarmCount;
    }

}

@Data
class EventResult {
    private Page page;
    private List<Event> event_list;
}

@Data
class Page {
    private int total;
    private int number;
    private int size;
}