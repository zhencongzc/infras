package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.monitor.AirInfo;
import com.cmbc.infras.dto.monitor.Humidity;
import com.cmbc.infras.dto.monitor.UpsInfo;
import com.cmbc.infras.dto.rpc.Monitor;

import java.util.List;

public interface MonitorService {

    BaseResult<List<UpsInfo>> getUpss(String bankId) throws Exception;

    BaseResult<JSONObject> upss(String bankId) throws Exception;

    BaseResult<List<AirInfo>> getAirs(String bankId);

    BaseResult<List<Humidity>> getHumids(String bankId);

    List<Monitor> getMonitorList(String leafId);

    List<Monitor> getMonitorList(List<String> leafIds);

    List<Device> getAccountDevice(String account, Integer type);

    List<Device> getBankDevice(String bankId, int type);

    List<JSONObject> childBankData(String bankId);
}
