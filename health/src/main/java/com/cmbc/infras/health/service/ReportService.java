package com.cmbc.infras.health.service;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface ReportService {

    JSONObject findBank(int isAdmin, String name) throws Exception;

    List<JSONObject> energyStatistics(JSONObject param) throws Exception;

    void exportEnergy(HttpServletResponse response, JSONObject param) throws Exception;

    List<JSONObject> temperatureHumidity(JSONObject param) throws Exception;

    void exportTemperatureHumidity(HttpServletResponse response, JSONObject param) throws Exception;

    List<JSONObject> healthModel(JSONObject param);

    List<JSONObject> healthScore(JSONObject param) throws Exception;

    void exportHealthScore(HttpServletResponse response, JSONObject param) throws Exception;

    List<JSONObject> alarmStatistics(JSONObject param) throws Exception;

    void exportAlarmStatistics(HttpServletResponse response, JSONObject param) throws Exception;

    List<JSONObject> findForm(JSONObject param) throws Exception;

    JSONObject maintainDrill(JSONObject param) throws Exception;

    void exportMaintainDrill(HttpServletResponse response, JSONObject param) throws Exception;

}
