package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.ops.*;

import java.util.List;

public interface HeadMainService {

    BaseResult<List<JSONObject>> getPartol();

    BaseResult<List<JSONObject>> getWorkCheck();

    BaseResult<SiteStatis> getSiteStatis() throws Exception;

    BaseResult<AlarmRadar> getBankAlarmRadar(String bankId);

    BaseResult<List<Evaluate>> getEvaluate();

    BaseResult<List<Humiture>> getHumiture();

}
