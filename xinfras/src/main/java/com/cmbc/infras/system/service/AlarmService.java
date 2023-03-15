package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.LabelParam;
import com.cmbc.infras.dto.rpc.event.*;

import java.util.List;

public interface AlarmService {

    BaseResult<List<AlarmEvent>> getAllAlarms(JSONObject param, boolean noAccount);

    BaseResult<List<AlarmEvent>> getAllAlarm(EventParam param);

    BaseResult<List<AlarmEvent>> getHistoryAlarm(JSONObject param);

    BaseResult<Boolean> alarmAccept(AlarmAcceptParam param);

    BaseResult<Boolean> alarmConfirm(AlarmConfirmParam param, boolean noAccount);

    BaseResult<List<Label>> getLabels();

    BaseResult<Label> getLabel(Integer id);

    BaseResult<Boolean> labelCheck(Integer id);

    BaseResult<Label> addLabel(LabelParam label);

    BaseResult<Boolean> editLabel(LabelParam label);

    BaseResult<Boolean> delLabel(Integer id);

    BaseResult<List<Bank>> getLocations();

    List<JSONObject> findFastInput(int type);

    int addFastInput(int type, String content);

    void updateFastInput(int id, String content);

    void deleteFastInput(int id);
}
