package com.cmbc.infras.system.config;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.rpc.event.AlarmConfirmParam;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.system.service.AlarmService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 确认告警的定时任务
 * 告警界面，每分钟执行一次，获取最新已恢复的非紧急的告警，自动做确认操作，只处理5分钟前的告警
 */
@Component
public class ConfirmAlarmSchedule {

    @Resource
    private AlarmService alarmService;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void doTask() {
        //获取最新已恢复的非紧急的告警
        JSONObject param = new JSONObject();
        param.put("eventLevel", "2,3,4,5");//自动处理的告警等级
        param.put("recoverState", "1");
        param.put("processState", "0,1");
        List<AlarmEvent> allAlarm = alarmService.getAllAlarms(param, true).getData();
        //去掉5分钟内的告警
        Iterator<AlarmEvent> iterator = allAlarm.iterator();
        long time = System.currentTimeMillis() / 1000 - 60 * 5;//获取5分钟前的时间戳
        while (iterator.hasNext()) {
            AlarmEvent next = iterator.next();
            if (next.getEventTime() > time) iterator.remove();
        }
        //封装参数
        List<Event> eventList = new LinkedList<>();
        allAlarm.forEach(alarmEvent -> {
            Event j = new Event();
            j.setGuid(alarmEvent.getGuid());
            j.setResource_id(alarmEvent.getResourceId());
            eventList.add(j);
        });
        //确认告警
        if (!eventList.isEmpty()) {
            AlarmConfirmParam param2 = new AlarmConfirmParam();
            param2.setConfirm_by("系统管理员");
            param2.setConfirm_description("告警已恢复，自动确认111");
            param2.setConfirm_type(1);
            param2.setEvent_list(eventList);
            if (!eventList.isEmpty()) {
                alarmService.alarmConfirm(param2, true);
            }
        }
    }

}
