package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.CountDoneResult;
import com.cmbc.infras.dto.rpc.event.Event;

import java.util.HashMap;
import java.util.List;

public interface HistoryAlarmService {

    AlarmCount getHistoryAlarmCount(List<String> bankIds);

    HashMap<String, Integer> getHistoryAlarmCount(List<String> bankIds, String group);

    AlarmCount getHistoryAlarmCount(List<String> bankIds, String levels, String group);

    List<Event> getHistoryAlarmData(List<String> bankIds, int pageNo, int pageSize);

    /**
     * 获取告警发生和恢复的数量，使用label标签过滤告警
     */
    CountDoneResult countDone(List<String> bankIds, String account);

    /**
     * 获取告警发生和恢复的数量，自定义等级过滤告警
     */
    CountDoneResult countDoneWithLevel(List<String> bankIds, String levels);
}
