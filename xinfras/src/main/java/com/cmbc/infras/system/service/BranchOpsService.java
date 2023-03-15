package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.event.AlarmInfo;
import com.cmbc.infras.dto.ops.Asset;
import com.cmbc.infras.dto.ops.OpsBankInfo;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;

import java.util.List;

public interface BranchOpsService {

    BaseResult<List<Bank>> getAllBanks(BaseParam param);

    BaseResult<OpsBankInfo> getBankInfos(BaseParam param) throws Exception;

    BaseResult<List<AlarmEvent>> getAlarmInfos(BaseParam param);

    BaseResult<List<Bank>> getLowerBank(BaseParam param);

    BaseResult<JSONObject> getBankAsset(BaseParam param) throws Exception;

}
