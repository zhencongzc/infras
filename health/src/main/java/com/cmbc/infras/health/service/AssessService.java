package com.cmbc.infras.health.service;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface AssessService {

    List<JSONObject> quickFind(int isAdmin, int isAuditRole, String name, String word);

    JSONObject scoreRank(String modelId);

    JSONObject historyScoreRank(String modelId, int version);

    JSONObject assessDimension(String modelId, String name, int isAdmin, int isAuditRole);

    List<JSONObject> record(String modelId, String name, int id, int year);

    List<JSONObject> findSingle(String modelId, String id, String name, int year);

    BaseResult<String> uploadDocument(MultipartFile file, int id);

    void downloadDocument(HttpServletResponse response, String document, String fileName);

    void commitSingle(JSONObject param);

    BaseResult<String> deleteDocument(int id, String document);

    List<JSONObject> findSingleAudit(String modelId, String id, String name, int year);

    void commitSingleAudit(JSONObject param);

    List<JSONObject> findDeduct(String modelId, String id, String name, int year);

    List<JSONObject> getSourceData(JSONObject param) throws Exception;

    void deleteSourceData(JSONObject param);

    void commitDeduct(JSONObject param);

    List<JSONObject> findDeductAudit(String modelId, String id, String name, int year);

    void updateFormData(JSONObject param) throws Exception;

    void saveStandardValue(JSONObject param);

    void commitDeductAudit(JSONObject param);

    List<JSONObject> findMonitor(String modelId, String id, String name, int year);

    List<JSONObject> findAnalysis(String modelId, String id, String name, int year);

    List<JSONObject> historyQuickFind(String name, String word, int start, int pageSize);

    List<JSONObject> adminHistoryQuickFind(String word, int start, int pageSize);

    List<JSONObject> getAssessTotal(String name, String word, int start, int pageSize);

    List<JSONObject> adminGetAssessTotal(String word, int start, int pageSize);

    JSONObject assessResult(int id) throws Exception;

    void checkReport(HttpServletResponse response, String modelId, String name);

    void correctExport(HttpServletResponse response, JSONObject param);

    void export(HttpServletResponse response, int id);

    void exportZipNow(HttpServletResponse response, String modelId);

    void exportZip(HttpServletResponse response, String modelId, Integer version);

}
