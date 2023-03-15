package com.cmbc.infras.health.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;

import java.util.List;

public interface ModelService {

    List<JSONObject> quickFind(String word, int start, int end);

    int getModelTotal(String word);

    void startModel(String modelId, int startModel) throws Exception;

    void startScore(String modelId, int startScore);

    List<JSONObject> findType();

    void addModel(JSONObject param, String modelId, String createTime) throws Exception;

    JSONObject findModel(String modelId);

    void deleteModel(String modelId);

    void updateModel(JSONObject param) throws Exception;

    BaseResult<JSONObject> findOrganizationAndRole();

    BaseResult<List<JSONObject>> findResource();

    BaseResult<List<String>> findFormState(String resourceId);

    BaseResult<List<JSONObject>> findMonitorList(JSONObject param);

    BaseResult<List<JSONObject>> findAnalysisList(JSONObject param);

    JSONObject addDimension(JSONObject param);

    void saveDimension(JSONObject param);

    void deleteDimension(JSONObject param);
}
