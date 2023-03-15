package com.cmbc.infras.dto.health;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * “告警信息”请求参数
 */
@Data
public class AlarmRequestParam {

    private static JSONObject page;
    private static List<JSONObject> sorts;
    private List<JSONObject> where;

    static {
        page = new JSONObject();
        page.put("number", 1);
        page.put("size", 1000000);
        JSONObject j = new JSONObject();
        j.put("field", "event_time");
        j.put("type", "DESC");
        sorts = new LinkedList<>();
        sorts.add(j);
    }

    public AlarmRequestParam(String[] bankIdArr, long start, long end, int[] eventLevel) {
        JSONObject j1 = new JSONObject();
        List<TermParam> terms = new LinkedList<>();
        TermParam termParam2 = new TermParam("event_time", "gte", start);
        TermParam termParam3 = new TermParam("event_time", "lte", end);
        TermParam termParam4 = new TermParam("event_level", "in", eventLevel);
        TermParam termParam5 = new TermParam("masked", "in", new int[]{0});
        terms.add(new TermParam("resource_id", "in", bankIdArr));
        terms.add(termParam2);
        terms.add(termParam3);
        terms.add(termParam4);
        terms.add(termParam5);
        j1.put("terms", terms);
        where = new LinkedList<>();
        where.add(j1);
        JSONObject j2 = new JSONObject();
        List<TermParam> terms2 = new LinkedList<>();
        String[] arr = new String[bankIdArr.length];
        for (int i = 0; i < bankIdArr.length; i++) {
            arr[i] = "%/" + bankIdArr[i] + "/%";
        }
        terms2.add(new TermParam("event_location", "like_any", arr));
        terms2.add(termParam2);
        terms2.add(termParam3);
        terms2.add(termParam4);
        terms2.add(termParam5);
        j2.put("terms", terms2);
        where.add(j2);
        JSONObject j3 = new JSONObject();
        List<TermParam> terms3 = new LinkedList<>();
        String[] arr2 = new String[bankIdArr.length];
        for (int i = 0; i < bankIdArr.length; i++) {
            arr2[i] = "%/" + bankIdArr[i];
        }
        terms3.add(new TermParam("event_location", "like_any", arr2));
        terms3.add(termParam2);
        terms3.add(termParam3);
        terms3.add(termParam4);
        terms3.add(termParam5);
        j3.put("terms", terms3);
        where.add(j3);
    }
}
