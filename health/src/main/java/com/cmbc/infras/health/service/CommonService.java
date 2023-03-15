package com.cmbc.infras.health.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;

import java.util.List;

public interface CommonService {

    BaseResult<JSONObject> findAuthority(int id);

    BaseResult<List<JSONObject>> evaluation(String modelId);

    BaseResult<JSONObject> maintainRate(String modelId, String name);
}
