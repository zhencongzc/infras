package com.cmbc.infras.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.FLowFormStatusEnum;
import java.util.List;

/**
 * 流程表单参数创建工具
 */
public class FLowFormParamUtil {

    /**
     * 创建流程表单参数
     */
    public static JSONObject createFormParam(String configDataId, String startTime, String endTime) {
        JSONObject param = new JSONObject();
        param.put("configDataId", configDataId);
        //query
        JSONObject query = new JSONObject();
        query.put("keyword", "");
        query.put("order", new JSONObject());
        query.put("page", 1);
        query.put("size", 100000);
        //search
        JSONObject search = new JSONObject();
        JSONObject time = new JSONObject();
        time.put("startTime", startTime);
        time.put("endTime", endTime);
        search.put("Field_xxx_create_time", time);
        query.put("search", search);
        param.put("query", query);
        return param;
    }


    /**
     * 演练任务参数
     */
    public static JSONObject createDeduceParam(List<Integer> sids, String startTime, String endTime) {
        return createDeduceParam(sids, startTime, endTime, 100000);
    }

    public static JSONObject createDeduceParam(List<Integer> sids, String startTime, String endTime, int pageSize) {
        JSONObject status = createParamStatus(sids);
        JSONObject time = new JSONObject();
        time.put("start", startTime);
        time.put("end", endTime);
        JSONObject search = new JSONObject();
        search.put("status", status);
        //执行开始时间-演练任务-Field_xxx_gppnof
        search.put("Field_xxx_mexcjyfxt", time);
        JSONObject query = new JSONObject();
        query.put("keyword", "");
        query.put("search", search);
        query.put("order", new JSONObject());
        query.put("page", 1);
        query.put("size", pageSize);
        JSONObject param = new JSONObject();
        param.put("configDataId", YmlConfig.deduceFormId);
        param.put("query", query);
        return param;
    }

    /**
     * 维护任务参数
     */
    public static JSONObject createMaintainParam(List<Integer> sids, String startTime, String endTime) {
        return createMaintainParam(sids, startTime, endTime, 100000);
    }

    public static JSONObject createMaintainParam(List<Integer> sids, String startTime, String endTime, int pageSize) {
        JSONObject status = createParamStatus(sids);
        JSONObject time = new JSONObject();
        time.put("startTime", startTime);
        time.put("endTime", endTime);
        JSONObject search = new JSONObject();
        search.put("status", status);
        search.put("Field_xxx_create_time", time);
        JSONObject query = new JSONObject();
        query.put("keyword", "");
        query.put("search", search);
        query.put("order", new JSONObject());
        query.put("page", 1);
        query.put("size", pageSize);
        JSONObject param = new JSONObject();
        param.put("configDataId", YmlConfig.maintainFormId);
        param.put("query", query);
        return param;
    }

    /**
     * 巡检动态参数
     */
    public static JSONObject createPartolParam(List<Integer> sids) {
        return createPartolParam(sids, 100000);
    }

    public static JSONObject createPartolParam(List<Integer> sids, int pageSize) {
        String start = DateTimeUtils.getTodayZeroFormat("yyyy-MM-dd HH:mm:ss");
        String end = DateTimeUtils.getCurrentFormat("yyyy-MM-dd HH:mm:ss");
        JSONObject status = createParamStatus(sids);
        //查询时间范围当日0时~当前时刻
        JSONObject time = new JSONObject();
        time.put("startTime", start);
        time.put("endTime", end);
        //执行开始时间-查询字段
        JSONObject search = new JSONObject();
        search.put("status", status);
        search.put("Field_xxx_create_time", time);
        JSONObject query = new JSONObject();
        query.put("keyword", "");
        query.put("search", search);
        query.put("order", new JSONObject());
        query.put("page", 1);
        query.put("size", pageSize);
        JSONObject param = new JSONObject();
        param.put("configDataId", YmlConfig.partolFormId);
        param.put("query", query);
        return param;
    }


    /**
     * 流程表单参数中status部分的创建抽出来
     */
    public static JSONObject createParamStatus(List<Integer> sids) {
        JSONObject status = new JSONObject();
        StringBuffer sb = new StringBuffer();
        JSONArray label = new JSONArray();
        for (Integer id : sids) {
            sb.append(FLowFormStatusEnum.getValue(id)).append(",");
            label.add(FLowFormStatusEnum.getLabel(id));
        }
        if (sb.length() > 0) {
            sb = sb.deleteCharAt(sb.length() - 1);
        }
        status.put("value", sb.toString());
        status.put("label", label);
        return status;
    }

}
