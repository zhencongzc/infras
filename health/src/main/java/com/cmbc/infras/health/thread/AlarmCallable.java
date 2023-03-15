package com.cmbc.infras.health.thread;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.health.dto.AlarmRequestParam;
import com.cmbc.infras.health.rpc.ProcessEngineRpc;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * 告警统计线程
 */
public class AlarmCallable implements Callable<List<JSONObject>> {

    private ProcessEngineRpc processEngineRpc;
    private JSONObject jsonObject;
    private String[] bankIdArr;
    private CountDownLatch cdl;

    public AlarmCallable(ProcessEngineRpc processEngineRpc, JSONObject jsonObject, String[] bankIdArr, CountDownLatch cdl) {
        this.processEngineRpc = processEngineRpc;
        this.jsonObject = jsonObject;
        this.bankIdArr = bankIdArr;
        this.cdl = cdl;
    }

    @Override
    public List<JSONObject> call() throws Exception {
        //组装参数，获取告警信息
        JSONArray listEventLevel = jsonObject.getJSONArray("eventLevel");
        int[] eventLevel = new int[listEventLevel.size()];
        for (int j = 0; j < listEventLevel.size(); j++) {
            eventLevel[j] = listEventLevel.getIntValue(j);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long start = sdf.parse(jsonObject.getString("dateStart")).getTime() / 1000;
        long end = sdf.parse(jsonObject.getString("dateEnd")).getTime() / 1000;
        AlarmRequestParam param1 = new AlarmRequestParam(bankIdArr, start, end, eventLevel);
        String result1 = processEngineRpc.getAlarmData(InfrasConstant.KE_RPC_COOKIE, param1);
        JSONObject json1 = JSONObject.parseObject(result1);
        if (!"00".equals(json1.getString("error_code")))
            throw new Exception("查询KE失败，接口：/api/v2/tsdb/status/event，返回信息：" + json1.toJSONString());
        List<JSONObject> eventList = json1.getJSONObject("data").getJSONArray("event_list").toJavaList(JSONObject.class);
        cdl.countDown();
        return eventList;
    }
}
