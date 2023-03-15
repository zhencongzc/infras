package com.cmbc.infras.system.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.system.mapper.AssetInfoMapper;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.service.AssetInfoService;
import com.cmbc.infras.system.util.InfoUtils;
import com.cmbc.infras.util.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
@Slf4j
public class AssetInfoServiceImpl implements AssetInfoService {

    @Resource
    private AssetInfoMapper assetInfoMapper;

    @Resource
    private EventRpc eventRpc;

    @Override
    public List<JSONObject> quickFind(String word, int start, int end, int synchronize) {
        List<JSONObject> list = assetInfoMapper.quickFind(word, start, end, synchronize);
        //查询需要展示的字段，按照asset_key_map表映射字段
        List<JSONObject> keyMapList = assetInfoMapper.findAssetKeyMap();
        HashMap<String, String> keyMap = new HashMap<>();
        keyMapList.forEach(a -> keyMap.put(a.getString("oldKey"), a.getString("newKey")));
        //通过asset_translate表对字段进行翻译
        List<JSONObject> translateList = assetInfoMapper.findAssetTranslate();
        HashMap<String, HashMap<String, String>> translateMap = new HashMap<>();
        translateList.forEach(a -> {
            String columnName = a.getString("columnName");
            if (translateMap.containsKey(columnName)) {
                translateMap.get(columnName).put(a.getString("syncValue"), a.getString("mapValue"));
            } else {
                HashMap<String, String> keyValueMap = new HashMap<>();
                keyValueMap.put(a.getString("syncValue"), a.getString("mapValue"));
                translateMap.put(columnName, keyValueMap);
            }
        });
        //字段映射和转义
        List<JSONObject> res = new LinkedList<>();
        for (JSONObject j : list) {
            JSONObject temp = new JSONObject();
            Set<String> strings = j.keySet();
            for (String s : strings) {
                String value = j.getString(s);
                //如果需要转义
                if (translateMap.containsKey(s)) value = translateMap.get(s).get(value);
                //如果需要映射字段
                if (keyMap.containsKey(s)) temp.put(keyMap.get(s), value);
            }
            res.add(temp);
        }
        return res;
    }

    @Override
    public int getModelTotal(String word, int synchronize) {
        return assetInfoMapper.getModelTotal(word, synchronize);
    }

    @Override
    public List<JSONObject> getAllData(String getDataUrl) {
        HttpRequest post = HttpUtil.createPost(getDataUrl);
        JSONObject json = new JSONObject();
        post.body(json.toJSONString());
        String body = post.execute().body();
        log.info("请求接口" + getDataUrl + ",入参body: {},返回参数body: {}", json, body);
        JSONObject res = JSONObject.parseObject(body);
        return res.getJSONObject("reply").getJSONArray("machineroomEquipList").toJavaList(JSONObject.class);
    }

    @Override
    @Transactional
    public void synchronizeAsset(List<JSONObject> data) {
        //获取resourceId信息
        List<JSONObject> list = assetInfoMapper.findAssetResourceId();
        HashMap<String, String> map = new HashMap<>();
        list.forEach(a -> map.put(a.getString("id"), a.getString("resourceId")));
        //获取synchronizeOrNot信息
        HashSet<String> set = assetInfoMapper.findAssetIdNeedSynchronize();
        //更新到新数据中
        data.forEach(a -> {
            String id = a.getString("id");
            if (map.containsKey(id)) {
                a.put("resourceId", map.get(id));
            } else {
                a.put("resourceId", "");
            }
            if (set.contains(id)) {
                a.put("synchronizeOrNot", 1);
            } else {
                a.put("synchronizeOrNot", 0);
            }
        });
        //删除全部数据
        assetInfoMapper.deleteAllAsset();
        //每次插入500数据
        for (int i = 0; i < data.size(); i += 500) {
            if (i + 500 <= data.size()) {
                assetInfoMapper.synchronizeAsset(data.subList(i, i + 500));
            } else {
                assetInfoMapper.synchronizeAsset(data.subList(i, data.size()));
                break;
            }
        }
    }

    @Override
    public void addAutoSynchronize(List<String> list) {
        if (!list.isEmpty()) assetInfoMapper.handleAutoSynchronize(list, 1);
    }

    @Override
    public void cancelAutoSynchronize(List<String> list) {
        if (!list.isEmpty()) assetInfoMapper.handleAutoSynchronize(list, 0);
    }

    @Override
    public void saveResourceId(String id, String resourceId) {
        assetInfoMapper.saveResourceId(id, resourceId);
    }

    @Override
    public String sendAsset(List<String> list1, HttpServletRequest request) {
        String res = "";
        //查询需要发送的数据
        List<JSONObject> list = assetInfoMapper.findAssetNeedSend(list1);
        if (!list.isEmpty()) {
            //查询需要展示的字段，按照asset_key_map表映射字段
            List<JSONObject> keyMapList = assetInfoMapper.findAssetKeyMap();
            HashMap<String, String> keyMap = new HashMap<>();
            keyMapList.forEach(a -> keyMap.put(a.getString("oldKey"), a.getString("newKey")));
            //通过asset_translate表对字段进行翻译
            List<JSONObject> translateList = assetInfoMapper.findAssetTranslate();
            HashMap<String, HashMap<String, String>> translateMap = new HashMap<>();
            translateList.forEach(a -> {
                String columnName = a.getString("columnName");
                if (translateMap.containsKey(columnName)) {
                    translateMap.get(columnName).put(a.getString("syncValue"), a.getString("mapValue"));
                } else {
                    HashMap<String, String> keyValueMap = new HashMap<>();
                    keyValueMap.put(a.getString("syncValue"), a.getString("mapValue"));
                    translateMap.put(columnName, keyValueMap);
                }
            });
            //字段映射和转义
            List<JSONObject> data = new LinkedList<>();
            for (JSONObject j : list) {
                JSONObject temp = new JSONObject();
                Set<String> strings = j.keySet();
                for (String s : strings) {
                    String value = j.getString(s);
                    //如果需要转义
                    if (translateMap.containsKey(s)) value = translateMap.get(s).get(value);
                    //如果需要映射字段
                    if (keyMap.containsKey(s)) temp.put(keyMap.get(s), value);
                }
                data.add(temp);
            }

            //将数据发送至资产管理系统
            JSONObject json = new JSONObject();
            json.put("list", data);
            res = eventRpc.sendAsset(InfoUtils.getCookieInfo(request), json);

            //更新"是否需要发送"、"发送状态"和"发送时间"等状态信息
            if ("success".equals(res)) {
                //发送成功
                assetInfoMapper.updateStateByList(list1, 0, 1, DateTimeUtils.getCurrentFormat());
            } else {
                assetInfoMapper.updateStateByList(list1, 1, 0, null);
            }
        }
        return res;
    }

    @Override
    public List<JSONObject> interfaceList(String word) {
        return assetInfoMapper.interfaceList(word);
    }

    @Override
    public void addInterface(String name, String url, String description) {
        assetInfoMapper.addInterface(name, url, description);
    }

    @Override
    public void saveInterface(int id, String name, String url, String description) {
        assetInfoMapper.saveInterface(id, name, url, description);
    }

    @Override
    public void deleteInterface(int id) {
        assetInfoMapper.deleteInterface(id);
    }

    @Override
    public String probeInterface(String url) throws Exception {
        HttpRequest post = HttpUtil.createPost(url);
        JSONObject jsonObject = new JSONObject();
        post.body(jsonObject.toJSONString());
        return post.execute().body();
    }

    @Override
    public List<JSONObject> dataList(String word, Integer sendOrNot) {
        return assetInfoMapper.dataList(word, sendOrNot);
    }

    @Override
    public void saveMapping(int id, String name, String newKey, int sendOrNot) {
        assetInfoMapper.saveMapping(id, name, newKey, sendOrNot);
    }

    @Override
    public List<JSONObject> mappingList(String oldKey) {
        return assetInfoMapper.mappingList(oldKey);
    }

    @Override
    public void addMapping(String name, String columnName, String syncValue, String mapValue, String description) throws Exception {
        List<JSONObject> list = assetInfoMapper.checkMappingExist(columnName, syncValue);
        if (!list.isEmpty()) throw new Exception("当前同步值已存在，请重新输入！");
        assetInfoMapper.addMapping(name, columnName, syncValue, mapValue, description);
    }

    @Override
    public void updateMapping(int id, String mapValue, String description) {
        assetInfoMapper.updateMapping(id, mapValue, description);
    }

    @Override
    public void deleteMapping(int id) {
        assetInfoMapper.deleteMapping(id);
    }

    @Override
    public void updateAsset(List<JSONObject> list) {
        //查询需要更新的数据，存入map
        List<JSONObject> needUpdate = assetInfoMapper.findAssetNeedUpdate(list);
        HashMap<String, JSONObject> objMap = new HashMap<>();
        list.forEach(a -> objMap.put(a.getString("resource_id"), a));

        //更新基础字段，更新properties字段
        if (needUpdate != null) {
            for (JSONObject obj : needUpdate) {
                //获取对应的对象
                JSONObject j = objMap.get(obj.getString("resource_id"));

                //将字段和值放入map
                Set<Map.Entry<String, Object>> entries = j.entrySet();
                HashMap<String, String> map = new HashMap<>();
                for (Map.Entry<String, Object> entry : entries) {
                    map.put(entry.getKey(), (String) entry.getValue());
                }

                //遍历obj的字段，更新基础字段的值
                Set<String> keys = obj.keySet();
                keys.forEach(a -> {
                    if (map.containsKey(a)) {
                        obj.put(a, map.get(a));
                        map.remove(a);
                    }
                });

                //更新properties字段
                if (!map.isEmpty()) {
                    JSONObject properties = obj.getJSONObject("properties");
                    //遍历properties的字段，更新对应值
                    Set<String> keys1 = properties.keySet();
                    for (String key : keys1) {
                        if (map.isEmpty()) break;
                        if (map.containsKey(key)) {
                            properties.put(key, map.get(key));
                            map.remove(key);
                        }
                    }
                    obj.put("properties", properties.toJSONString());
                }
            }
        }

        //删除已存在的数据
        assetInfoMapper.deleteAssetByList(needUpdate);
        //新增数据
        assetInfoMapper.insertAssetByList(needUpdate);
    }
}
