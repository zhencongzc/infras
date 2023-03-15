package com.cmbc.infras.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.AlarmResult;
import com.cmbc.infras.dto.rpc.alarm.FieldItem;
import com.cmbc.infras.dto.rpc.alarm.TermItem;
import com.cmbc.infras.dto.rpc.alarm.WhereCountIterm;
import com.cmbc.infras.dto.rpc.alarm.WhereDataItem;
import com.cmbc.infras.dto.rpc.event.Event;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 历史告警参数为
 * 实时告警以数类-EventParamUtils
 */
public class AlarmParamUtils {

    /**
     * 创建历史告警查询参数-单一resourceId
     * resourceId in ["0_728]
     * event_location like_any ["%/0_728/%"]
     * event_location like_any ["%/0_728"]
     * masked in [0]
     * 时间项 当日0时~当前时间
     * 参照::KE->报表报告->数据查询->告警查询-选中某行查询告警
     */
    public static WhereCountIterm createHistoryCountParam(List<String> bankIds, String levels) {
        return createHistoryCountParam(bankIds, levels, false);
    }

    public static WhereCountIterm createHistoryCountParam(List<String> bankIds, String levels, String group) {
        return createHistoryCountParam(bankIds, levels, false, group);
    }

    public static WhereCountIterm createHistoryCountParam(List<String> bankIds, String levels, boolean done) {
        return createHistoryCountParam(bankIds, levels, done, null);
    }

    public static WhereCountIterm createHistoryCountParam(List<String> bankIds, String levels, boolean done, String group) {
        WhereCountIterm where = new WhereCountIterm(createWhereTerms(bankIds, levels, done));
        //固定count参数"group": "event_level"
        if (StringUtils.isBlank(group)) {
            where.setGroup("event_level");
        } else {
            where.setGroup(group);
        }
        return where;
    }

    public static WhereDataItem createHistoryDataParam(List<String> bankIds, String levels, int pageNo, int pageSize) {
        //获取最近一个月的告警
        WhereDataItem where = new WhereDataItem(createWhereTermsWithDate(bankIds, levels, DateTimeUtils.getCurrentTime() - 3600 * 24 * 30,
                DateTimeUtils.getCurrentTime()));
        where.setPage(pageNo, pageSize);
        //固定查询参数"field":"event_time","type": "DESC"
        where.setSort("event_time", "DESC");
        return where;
    }

    /**
     * done:是否已完成 或 已恢复 统计数据用到了
     */
    public static List<TermItem> createWhereTerms(List<String> ids, String levels, boolean done) {
        List<String> bothIds = new ArrayList<>();
        List<String> preIds = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            ids = new ArrayList<>();
        }
        for (String id : ids) {
            bothIds.add("%/" + id + "/%");
            preIds.add("%/" + id);
        }
        List<TermItem> terms = new ArrayList<>();
        //共用项
        FieldItem gteItem = new FieldItem("event_time", "gte", DateTimeUtils.getTodayZeroDot());
        FieldItem lteItem = new FieldItem("event_time", "lte", DateTimeUtils.getCurrentTime());
        FieldItem maskItem = new FieldItem("masked", "in", Arrays.asList(0));
        FieldItem eventTypeItem = new FieldItem("event_type", "in", Arrays.asList(0, 2, 3, 4, 5, 21));//告警类型
        //Term1
        FieldItem resourceItem = new FieldItem("resource_id", "in", ids);
        //Term2
        FieldItem eventItemAll = new FieldItem("event_location", "like_any", bothIds);
        //Term3
        FieldItem eventItemPre = new FieldItem("event_location", "like_any", preIds);
        TermItem term1 = new TermItem().add(resourceItem).add(gteItem).add(lteItem).add(maskItem).add(eventTypeItem);
        TermItem term2 = new TermItem().add(eventItemAll).add(gteItem).add(lteItem).add(maskItem).add(eventTypeItem);
        TermItem term3 = new TermItem().add(eventItemPre).add(gteItem).add(lteItem).add(maskItem).add(eventTypeItem);
        //告警级别"1,2"
        if (StringUtils.isNotBlank(levels)) {
            String[] split = levels.split(",");
            int[] level = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                level[i] = Integer.parseInt(split[i]);
            }
            FieldItem levelItem = new FieldItem("event_level", "in", level);
            term1.add(levelItem);
            term2.add(levelItem);
            term3.add(levelItem);
        }
        terms.add(term1);
        terms.add(term2);
        terms.add(term3);
        return terms;
    }

    public static List<TermItem> createWhereTermsWithDate(List<String> ids, String levels, long dateStart, long dateEnd) {
        List<String> bothIds = new ArrayList<>();
        List<String> preIds = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            ids = new ArrayList<>();
        }
        for (String id : ids) {
            bothIds.add("%/" + id + "/%");
            preIds.add("%/" + id);
        }
        List<TermItem> terms = new ArrayList<>();
        //共用项
        FieldItem gteItem = new FieldItem("event_time", "gte", dateStart);
        FieldItem lteItem = new FieldItem("event_time", "lte", dateEnd);
        FieldItem maskItem = new FieldItem("masked", "in", Arrays.asList(0));
        FieldItem eventTypeItem = new FieldItem("event_type", "in", Arrays.asList(0, 2, 3, 4, 5, 21));//告警类型
        //Term1
        FieldItem resourceItem = new FieldItem("resource_id", "in", ids);
        //Term2
        FieldItem eventItemAll = new FieldItem("event_location", "like_any", bothIds);
        //Term3
        FieldItem eventItemPre = new FieldItem("event_location", "like_any", preIds);
        TermItem term1 = new TermItem().add(resourceItem).add(gteItem).add(lteItem).add(maskItem).add(eventTypeItem);
        TermItem term2 = new TermItem().add(eventItemAll).add(gteItem).add(lteItem).add(maskItem).add(eventTypeItem);
        TermItem term3 = new TermItem().add(eventItemPre).add(gteItem).add(lteItem).add(maskItem).add(eventTypeItem);
        //告警级别"1,2"
        if (StringUtils.isNotBlank(levels)) {
            String[] split = levels.split(",");
            int[] level = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                level[i] = Integer.parseInt(split[i]);
            }
            FieldItem levelItem = new FieldItem("event_level", "in", level);
            term1.add(levelItem);
            term2.add(levelItem);
            term3.add(levelItem);
        }
        terms.add(term1);
        terms.add(term2);
        terms.add(term3);
        return terms;
    }

    public static AlarmCount parseCountResult(String countStr) {
        JSONObject countObj = JSONObject.parseObject(countStr);
        String countCode = countObj.getString("error_code");
        if (!"00".equals(countCode)) {
            throw new RuntimeException(String.format("AlarmParamUtils.parseCountResult error! str is %s", countStr));
        }
        String countData = countObj.getString("data");
        AlarmCount alarmCount = JSON.parseObject(countData, AlarmCount.class);
        return alarmCount;
    }

    public static List<Event> parseAlarmResult(String str) {
        JSONObject obj = JSONObject.parseObject(str);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) {
            throw new RuntimeException(String.format("AlarmParamUtils.parseAlarmResult error! str is %s", str));
        }
        String data = obj.getString("data");
        AlarmResult alarmResult = JSON.parseObject(data, AlarmResult.class);
        return alarmResult.getEvent_list();
    }

    public static String createCookie(String account) {
        return "DCIM_ACCOUNT=" + account;
    }

}
