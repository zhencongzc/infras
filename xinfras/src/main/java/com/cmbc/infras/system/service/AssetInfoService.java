package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface AssetInfoService {

    List<JSONObject> quickFind(String word, int start, int end, int synchronize);

    int getModelTotal(String word, int synchronize);

    List<JSONObject> getAllData(String getDataUrl);

    void synchronizeAsset(List<JSONObject> data);

    void addAutoSynchronize(List<String> list);

    void cancelAutoSynchronize(List<String> list);

    void saveResourceId(String id, String resourceId);

    String sendAsset(List<String> list, HttpServletRequest request);

    List<JSONObject> interfaceList(String word);

    void addInterface(String name, String url, String description);

    void saveInterface(int id, String name, String url, String description);

    void deleteInterface(int id);

    String probeInterface(String url) throws Exception;

    List<JSONObject> dataList(String word, Integer sendOrNot);

    void saveMapping(int id, String name, String newKey, int sendOrNot);

    List<JSONObject> mappingList(String oldKey);

    void addMapping(String name, String columnName, String syncValue, String mapValue, String description) throws Exception;

    void updateMapping(int id, String mapValue, String description);

    void deleteMapping(int id);

    void updateAsset(List<JSONObject> list);
}
