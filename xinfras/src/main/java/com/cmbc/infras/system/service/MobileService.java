package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.rpc.event.AlarmAcceptParam;
import com.cmbc.infras.dto.rpc.event.AlarmConfirmParam;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;

import java.util.HashMap;
import java.util.List;

public interface MobileService {

    BaseResult<List<Bank>> getBanks(BaseParam param);

    BaseResult<Boolean> confirm(AlarmConfirmParam param);

    BaseResult<Boolean> accept(AlarmAcceptParam param);

    BaseResult<List<AlarmEvent>> getAlarms(Label label);

    List<Bank> getSubBanks();

    BaseResult<List<JSONObject>> upss(List<Bank> list) throws Exception;

    HashMap<String, Integer> alarmCount(JSONObject json);

    List<JSONObject> getUpss(String bankId) throws Exception;

}
