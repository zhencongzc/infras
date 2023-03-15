package com.cmbc.infras.system.service.impl;

import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.CountDoneResult;
import com.cmbc.infras.dto.rpc.alarm.FieldItem;
import com.cmbc.infras.dto.rpc.alarm.TermItem;
import com.cmbc.infras.dto.rpc.alarm.WhereCountIterm;
import com.cmbc.infras.dto.rpc.alarm.WhereDataItem;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.system.rpc.HistoryAlarmRpc;
import com.cmbc.infras.system.service.HistoryAlarmService;
import com.cmbc.infras.system.service.LabelService;
import com.cmbc.infras.util.AlarmParamUtils;
import com.cmbc.infras.util.UserContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoryAlarmServiceImpl implements HistoryAlarmService {

    @Resource
    private HistoryAlarmRpc historyAlarmRpc;

    @Resource
    private LabelService labelService;

    @Override
    public AlarmCount getHistoryAlarmCount(List<String> bankIds) {
        String account = UserContext.getUserAccount();
        Label label = labelService.getUserLabel(account);
        String eventLevel = label.getEventLevel();
        return getHistoryAlarmCount(bankIds, eventLevel, null);
    }

    @Override
    public HashMap<String, Integer> getHistoryAlarmCount(List<String> bankIds, String group) {
        String account = UserContext.getUserAccount();
        String levels = "1,2,3,4,5";
        String cookie = AlarmParamUtils.createCookie(account);
        //未处理：未处理的
        WhereCountIterm whereParam = AlarmParamUtils.createHistoryCountParam(bankIds, levels, group);
        for (TermItem item : whereParam.getWhere()) {
            item.add(new FieldItem("is_accept", "in", new int[]{0}));
        }
        String str = historyAlarmRpc.getHistoryAlarmCount(cookie, whereParam);
        AlarmCount alarmCount = AlarmParamUtils.parseCountResult(str);
        //处理中：已受理的
        WhereCountIterm whereParam2 = AlarmParamUtils.createHistoryCountParam(bankIds, levels, group);
        for (TermItem item : whereParam2.getWhere()) {
            item.add(new FieldItem("is_accept", "in", new int[]{1}));
        }
        String str2 = historyAlarmRpc.getHistoryAlarmCount(cookie, whereParam2);
        AlarmCount alarmCount2 = AlarmParamUtils.parseCountResult(str2);
        //已处理：已确认的
        WhereCountIterm whereParam3 = AlarmParamUtils.createHistoryCountParam(bankIds, levels, group);
        for (TermItem item : whereParam3.getWhere()) {
            item.add(new FieldItem("is_accept", "in", new int[]{2}));
        }
        String str3 = historyAlarmRpc.getHistoryAlarmCount(cookie, whereParam3);
        AlarmCount alarmCount3 = AlarmParamUtils.parseCountResult(str3);
        //返回参数
        HashMap<String, Integer> res = new HashMap<>();
        res.put("not", alarmCount.getCount());
        res.put("being", alarmCount2.getCount());
        res.put("already", alarmCount3.getCount());
        return res;
    }

    @Override
    public AlarmCount getHistoryAlarmCount(List<String> bankIds, String levels, String group) {
        String account = UserContext.getUserAccount();
        WhereCountIterm whereParam = AlarmParamUtils.createHistoryCountParam(bankIds, levels, group);
        String cookie = AlarmParamUtils.createCookie(account);
        String str = historyAlarmRpc.getHistoryAlarmCount(cookie, whereParam);
        AlarmCount alarmCount = AlarmParamUtils.parseCountResult(str);
        return alarmCount;
    }

    @Override
    public List<Event> getHistoryAlarmData(List<String> bankIds, int pageNo, int pageSize) {
        String account = UserContext.getUserAccount();
        Label label = labelService.getUserLabel(account);
        String levels = label.getEventLevel();
        WhereDataItem whereParam = AlarmParamUtils.createHistoryDataParam(bankIds, levels, pageNo, pageSize);
        String cookie = AlarmParamUtils.createCookie(account);
        String str = historyAlarmRpc.getHistoryAlarm(cookie, whereParam);
        List<Event> alarms = AlarmParamUtils.parseAlarmResult(str);
        return alarms;
    }

    @Override
    public CountDoneResult countDone(List<String> bankIds, String account) {
        Label label = labelService.getUserLabel(account);
        String levels = label.getEventLevel();
        //统计紧急和严重告警数量
        WhereCountIterm countParam = AlarmParamUtils.createHistoryCountParam(bankIds, levels);
        String countStr = historyAlarmRpc.getHistoryAlarmCount(AlarmParamUtils.createCookie(account), countParam);
        AlarmCount count = AlarmParamUtils.parseCountResult(countStr);
        //统计紧急和严重告警，条件是处理状态为”已处理“或“处理中”，或者处理状态为”未处理“但恢复状态为”已恢复“的数量
        //1，统计”已处理“或“处理中”的
        WhereCountIterm countDoneParam1 = AlarmParamUtils.createHistoryCountParam(bankIds, levels, true);
        for (TermItem termItem : countDoneParam1.getWhere()) {
            FieldItem acceptItem = new FieldItem("is_accept", "in", Arrays.asList(1, 2));
            termItem.add(acceptItem);
        }
        String countDontStr1 = historyAlarmRpc.getHistoryAlarmCount(AlarmParamUtils.createCookie(account), countDoneParam1);
        AlarmCount countDone1 = AlarmParamUtils.parseCountResult(countDontStr1);
        //2，统计”未处理“但”已恢复“的
        WhereCountIterm countDoneParam2 = AlarmParamUtils.createHistoryCountParam(bankIds, levels, true);
        for (TermItem termItem : countDoneParam2.getWhere()) {
            FieldItem recoverItem = new FieldItem("is_recover", "in", Arrays.asList(1, 2));
            FieldItem acceptItem = new FieldItem("is_accept", "in", Arrays.asList(0));
            termItem.add(recoverItem).add(acceptItem);
        }
        String countDontStr2 = historyAlarmRpc.getHistoryAlarmCount(AlarmParamUtils.createCookie(account), countDoneParam2);
        AlarmCount countDone2 = AlarmParamUtils.parseCountResult(countDontStr2);
        //3，两种数据加和
        AlarmCount countDone = combine(countDone1, countDone2);
        return new CountDoneResult(count, countDone);
    }

    @Override
    public CountDoneResult countDoneWithLevel(List<String> bankIds, String levels) {
        String account = "admin";
        //统计紧急、严重、重要的告警数量
        WhereCountIterm countParam = AlarmParamUtils.createHistoryCountParam(bankIds, levels);
        String countStr = historyAlarmRpc.getHistoryAlarmCount(AlarmParamUtils.createCookie(account), countParam);
        AlarmCount count = AlarmParamUtils.parseCountResult(countStr);
        //1，统计处理状态为”已处理“或“处理中”的
        WhereCountIterm countDoneParam1 = AlarmParamUtils.createHistoryCountParam(bankIds, levels, true);
        for (TermItem termItem : countDoneParam1.getWhere()) {
            FieldItem acceptItem = new FieldItem("is_accept", "in", Arrays.asList(1, 2));
            termItem.add(acceptItem);
        }
        String countDontStr1 = historyAlarmRpc.getHistoryAlarmCount(AlarmParamUtils.createCookie(account), countDoneParam1);
        AlarmCount countDone1 = AlarmParamUtils.parseCountResult(countDontStr1);
        //2，统计处理状态为”未处理“但恢复状态为”已恢复“的
        WhereCountIterm countDoneParam2 = AlarmParamUtils.createHistoryCountParam(bankIds, levels, true);
        for (TermItem termItem : countDoneParam2.getWhere()) {
            FieldItem recoverItem = new FieldItem("is_recover", "in", Arrays.asList(1, 2));
            FieldItem acceptItem = new FieldItem("is_accept", "in", Arrays.asList(0));
            termItem.add(recoverItem).add(acceptItem);
        }
        String countDontStr2 = historyAlarmRpc.getHistoryAlarmCount(AlarmParamUtils.createCookie(account), countDoneParam2);
        AlarmCount countDone2 = AlarmParamUtils.parseCountResult(countDontStr2);
        //3，两种数据加和
        AlarmCount countDone = combine(countDone1, countDone2);
        return new CountDoneResult(count, countDone);
    }

    private AlarmCount combine(AlarmCount countDone1, AlarmCount countDone2) {
        AlarmCount res = new AlarmCount();
        res.setCount(countDone1.getCount() + countDone2.getCount());
        Map<String, Integer> group = new HashMap<>();
        Map<String, Integer> group1 = countDone1.getGroup();
        Map<String, Integer> group2 = countDone2.getGroup();
        group.put("1", (group1.get("1") == null ? 0 : group1.get("1")) + (group2.get("1") == null ? 0 : group2.get("1")));
        group.put("2", (group1.get("2") == null ? 0 : group1.get("2")) + (group2.get("2") == null ? 0 : group2.get("2")));
        group.put("3", (group1.get("3") == null ? 0 : group1.get("3")) + (group2.get("3") == null ? 0 : group2.get("3")));
        res.setGroup(group);
        return res;
    }

}
