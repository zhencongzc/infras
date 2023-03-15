package com.cmbc.infras.health.service;

import com.alibaba.fastjson.JSONObject;

import java.text.ParseException;
import java.util.List;

public interface StatisticsService {

    List<JSONObject> rating(String modelId) throws Exception;

    List<JSONObject> trend(String modelId) throws Exception;

    JSONObject overview(String modelId);

    List<JSONObject> rank(String modelId);

    List<JSONObject> scoreRate(String modelId);

    List<JSONObject> dimensionTrend(String modelId) throws Exception;

    List<JSONObject> branchScoreRate(String modelId, String name);

    List<JSONObject> branchScoreTrend(String modelId, String name) throws Exception;

    List<JSONObject> branchDimensionRank(String modelId, String name);

    JSONObject branchOverview(String modelId, String name);

    List<JSONObject> branchScoreDetail(String modelId, String name);

}
