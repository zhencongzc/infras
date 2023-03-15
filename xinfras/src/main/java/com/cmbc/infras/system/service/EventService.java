package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.EventInfo;
import com.cmbc.infras.dto.rpc.event.*;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface EventService {

    BaseResult<List<EventInfo>> events(String bankId);

    /**
     * 查询实时告警
     */
    BaseResult<List<Event>> getEventLast(EventParam param);

    /**
     * 查询历史告警
     */
    BaseResult<List<Event>> getEvents(String account, EventParam param);

    BaseResult<List<Event>> getEvents(String account, JSONObject param);

    BaseResult<Boolean> alarmAccept(AlarmAcceptParam param);

    BaseResult<Boolean> alarmConfirm(AlarmConfirmParam param);

    /**
     * 单独查数量接口-权限放开
     * 第三方需求
     */
    Integer getAllAlarmCount(String account, String token) throws Exception;

    BaseResult<AlarmCount> getEventLastCount(String bankId);

}
