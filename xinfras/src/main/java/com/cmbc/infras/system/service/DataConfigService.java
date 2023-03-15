package com.cmbc.infras.system.service;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface DataConfigService {

    List<JSONObject> quickFind(String word, int start, int end);

    int getTotalByWord(String word);

    List<JSONObject> advancedQuery(JSONObject query, int start, int end);

    int getTotalByQuery(JSONObject query);

    void createFastQuery(JSONObject param);

    List<JSONObject> findFastQuery(String account);

    void deleteFastQuery(int id);

    void createBank(JSONObject param);

    List<JSONObject> findBankId(JSONObject param) throws Exception;

    List<Integer> findSortPosition(JSONObject param);

    List<JSONObject> findBank(JSONObject param);

    void updateBank(JSONObject param);

    void deleteBank(JSONObject param);

    List<JSONObject> findDevice(JSONObject param);

    List<JSONObject> findSpotType(JSONObject param);

    void saveDevice(JSONObject param);

    JSONObject getBankSpotListFromKeByResourceId(String resourceId, JSONObject param);

    void initializeSafeTime();

    JSONObject findDeviceStatus(String bankId);

    List<JSONObject> userList(String word);

    List<JSONObject> findEmployee();

    void addUser(List<JSONObject> list);

    void deleteUser(int id);

    List<JSONObject> findFormList() throws Exception;

    void updateUser(JSONObject param);

    void mapOtherUser(JSONObject param);

    void informFormMessage(String moduleKey, String title, String context);
}
