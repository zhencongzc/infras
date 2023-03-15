package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.DeviceTypeEnum;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.constant.SpotTypeEnum;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.rpc.event.*;
import com.cmbc.infras.dto.rpc.spot.SpotDto;
import com.cmbc.infras.dto.rpc.spot.WhereDto;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.DataConfigMapper;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.rpc.FlowFormRpc;
import com.cmbc.infras.system.rpc.MobileOARpc;
import com.cmbc.infras.system.rpc.RpcUtil;
import com.cmbc.infras.system.service.DataConfigService;
import com.cmbc.infras.system.util.BusinessUtil;
import com.cmbc.infras.util.AlarmParamUtils;
import com.cmbc.infras.util.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import com.cmbc.infras.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class DataConfigServiceImpl implements DataConfigService {

    @Resource
    private DataConfigMapper dataConfigMapper;

    @Resource
    private EventRpc eventRpc;
    @Resource
    private FlowFormRpc flowFormRpc;
    @Resource
    private MobileOARpc mobileOARpc;

    @Override
    public List<JSONObject> quickFind(String word, int start, int end) {
        List<JSONObject> list = dataConfigMapper.quickFind(word, start, end);
        return list;
    }

    @Override
    public int getTotalByWord(String word) {
        return dataConfigMapper.getTotalByWord(word);
    }

    @Override
    public List<JSONObject> advancedQuery(JSONObject query, int start, int end) {
        List<JSONObject> list = dataConfigMapper.advancedQuery(query.getString("bankName"), query.getString("parentName"), query.getString("sort"),
                query.getString("level"), query.getString("contactId"), query.getString("contact"), query.getString("areaName"), start, end);
        return list;
    }

    @Override
    public int getTotalByQuery(JSONObject query) {
        return dataConfigMapper.getTotalByQuery(query.getString("bankName"), query.getString("parentName"), query.getString("sort"),
                query.getString("level"), query.getString("contactId"), query.getString("contact"), query.getString("areaName"));
    }

    @Override
    public void createFastQuery(JSONObject param) {
        dataConfigMapper.createFastQuery("bankInfo", param.getString("account"), param.getString("name"), param.getJSONObject("rule").toJSONString());
    }

    @Override
    public List<JSONObject> findFastQuery(String account) {
        List<JSONObject> list = dataConfigMapper.findFastQuery(account);
        for (JSONObject l : list) {
            l.put("rule", l.getJSONObject("rule"));
        }
        return list;
    }

    @Override
    public void deleteFastQuery(int id) {
        dataConfigMapper.deleteFastQuery(id);
    }

    @Override
    public List<JSONObject> findBankId(JSONObject param) throws Exception {
        String bankId = param.getString("bankId");
        String str = eventRpc.spaceView(InfrasConstant.KE_RPC_COOKIE, bankId, "ci_type", "2,3,5,6", "5"
                , "list");
        JSONArray array = JSONArray.parseArray(str);
        if (array == null) return new LinkedList<>();
        return array.toJavaList(JSONObject.class);
    }

    @Override
    public JSONObject getBankSpotListFromKeByResourceId(String resourceId, JSONObject param) {
        JSONObject result = new JSONObject();
        SpotDto spotDto = new SpotDto();
        spotDto.setResource_id(resourceId);
        spotDto.setRelation_code("5");
        spotDto.setPage(new PageCondition(param.getString("pageCount"), param.getInteger("pageSize")));

        List<QueryCondition> terms = new ArrayList<>();
        QueryCondition ci_type = new QueryCondition("ci_type", "eq", "3");
        QueryCondition spot_type = new QueryCondition("spot_type", "eq", param.getString("spot_type"));
        QueryCondition data_source = new QueryCondition("data_source", "eq", "0");
        terms.add(ci_type);
        terms.add(spot_type);
        terms.add(data_source);

        WhereDto where = new WhereDto();
        where.setTerms(terms);

        spotDto.setWhere(new ArrayList<WhereDto>() {{
            add(where);
        }});

        String spotInfoStr = eventRpc.spaceDeviceSpotList(InfrasConstant.KE_RPC_COOKIE, spotDto);
        JSONObject spotInfoJo = JSONObject.parseObject(spotInfoStr);
        String error_code = spotInfoJo.getString("error_code");
        if (!"00".equals(error_code)) {
            log.error("空间组态中设备的测点信息接口获取失败");
            return result;
        }
        result = spotInfoJo.getJSONObject("data");
        return result;
    }

    @Override
    public List<Integer> findSortPosition(JSONObject param) {
        String parentId = param.getString("parentId");
        //获取上级银行为parentId的全部sort
        List<Integer> res = dataConfigMapper.getSortByParentId(parentId);
        //最后增加1位作为前端默认值
        if (res.size() == 0) {
            res.add(1);
        } else {
            res.add(res.get(res.size() - 1) + 1);
        }
        return res;
    }

    @Override
    @Transactional
    public void createBank(JSONObject param) {
        //根据上级银行重排序
        String sort = param.getString("sort");
        String parentId = param.getString("parentId");
        dataConfigMapper.sortBank(parentId, sort);
        //入库
        dataConfigMapper.createBank(param.getString("bankId"), param.getString("bankName"), parentId,
                param.getString("parentName"), param.getString("contactId"), param.getString("contact"), sort,
                param.getString("level"), param.getString("levelName"), param.getString("lng"), param.getString("lat"),
                param.getString("areaId"), param.getString("areaName"), param.getString("linkId"), param.getString("remark"));
    }

    @Override
    public List<JSONObject> findBank(JSONObject param) {
        List<JSONObject> list = dataConfigMapper.findBankById(param.getString("id"));
        return list;
    }

    @Override
    @Transactional
    public void updateBank(JSONObject param) {
        //重排序
        String parentId = param.getString("parentId");
        int newSort = param.getIntValue("sort");
        int oldSort = dataConfigMapper.getSortById(param.getIntValue("id"));
        //往前移动将新旧顺序的区间值+1，往后移动将新旧顺序的区间值-1，
        if (newSort < oldSort) {
            dataConfigMapper.sortBankByRangePlus(parentId, newSort, oldSort);
        } else if (newSort > oldSort) {
            dataConfigMapper.sortBankByRangeMinus(parentId, newSort, oldSort);
        }
        //更新数据
        String bankId = param.getString("bankId");
        dataConfigMapper.updateBank(param.getString("id"), bankId, param.getString("bankName"), param.getString("parentId"),
                param.getString("parentName"), param.getString("contactId"), param.getString("contact"), param.getString("sort"),
                param.getString("level"), param.getString("levelName"), param.getString("lng"), param.getString("lat"),
                param.getString("areaId"), param.getString("areaName"), param.getString("linkId"), param.getString("remark"));
        //主动缓存
        String redisKey = InfrasConstant.INFRAS_BANK_INFO + bankId;
        JSONObject bank = new JSONObject();
        bank.put("bankId", param.getString("bankId"));
        bank.put("bankName", param.getString("bankName"));
        bank.put("parentId", param.getString("parentId"));
        bank.put("parentName", param.getString("parentName"));
        bank.put("contactId", param.getString("contactId"));
        bank.put("contact", param.getString("contact"));
        bank.put("sort", param.getString("sort"));
        bank.put("level", param.getString("level"));
        bank.put("levelName", param.getString("levelName"));
        bank.put("lng", param.getString("lng"));
        bank.put("lat", param.getString("lat"));
        bank.put("areaId", param.getString("areaId"));
        bank.put("areaName", param.getString("areaName"));
        bank.put("remark", param.getString("remark"));
        DataRedisUtil.addStringToRedis(redisKey, JSON.toJSONString(bank), InfrasConstant.TIME_OUT);
    }

    @Override
    public void deleteBank(JSONObject param) {
        JSONArray list = param.getJSONArray("list");
        if (list.size() != 0) {
            dataConfigMapper.deleteBank(list.toJavaList(JSONObject.class));
        }
    }

    @Override
    public List<JSONObject> findDevice(JSONObject param) {
        //层级结构：设备类型（固定）--设备信息--测点类型（固定）--测点信息
        List<JSONObject> res = new LinkedList<>();
        DeviceTypeEnum[] deviceTypes = DeviceTypeEnum.values();
        //设备类型
        for (DeviceTypeEnum deviceType : deviceTypes) {
            JSONObject j = new JSONObject();
            int type = deviceType.getType();
            j.put("deviceType", type);
            j.put("name", deviceType.getName());
            //设备信息
            List<JSONObject> devices = dataConfigMapper.findDeviceByBankIdAndType(param.getString("bankId"), type);
            for (JSONObject device : devices) {
                List<JSONObject> list = new LinkedList<>();
                //测点类型
                List<SpotTypeEnum> spotTypes = SpotTypeEnum.getListByDeviceType(type);
                for (SpotTypeEnum spotType : spotTypes) {
                    JSONObject j1 = new JSONObject();
                    int type1 = spotType.getType();
                    j1.put("spotType", type1);
                    j1.put("name", spotType.getName());
                    //测点信息
                    List<JSONObject> spots = dataConfigMapper.findSpotByDeviceIdAndType(device.getString("deviceId"), type1);
                    j1.put("spots", spots);
                    list.add(j1);
                }
                device.put("spotTypes", list);
            }
            j.put("devices", devices);
            res.add(j);
        }
        return res;
    }

    @Override
    public List<JSONObject> findSpotType(JSONObject param) {
        List<JSONObject> res = new LinkedList<>();
        List<SpotTypeEnum> spotTypes = SpotTypeEnum.getListByDeviceType(param.getIntValue("deviceType"));
        for (SpotTypeEnum spotType : spotTypes) {
            JSONObject j = new JSONObject();
            j.put("spotType", spotType.getType());
            j.put("name", spotType.getName());
            res.add(j);
        }
        return res;
    }

    @Override
    @Transactional
    public void saveDevice(JSONObject param) {
        String bankId = param.getString("bankId");
        //删除原来全部设备和测点
        List<JSONObject> list = dataConfigMapper.findDeviceByBankId(bankId);
        if (list.size() != 0) {
            dataConfigMapper.deleteSpotByDeviceId(list);
            dataConfigMapper.deleteDeviceByBankId(bankId);
        }
        //新建设备和测点
        List<JSONObject> data = param.getJSONArray("data").toJavaList(JSONObject.class);
        List<JSONObject> devices = new LinkedList<>();
        List<JSONObject> spots = new LinkedList<>();
        for (JSONObject d : data) {
            List<JSONObject> listDevices = d.getJSONArray("devices").toJavaList(JSONObject.class);
            if (listDevices.size() != 0) {
                //添加设备
                devices.addAll(listDevices);
                for (JSONObject dev : listDevices) {
                    List<JSONObject> spotTypes = dev.getJSONArray("spotTypes").toJavaList(JSONObject.class);
                    for (JSONObject spotType : spotTypes) {
                        List<JSONObject> listSpots = spotType.getJSONArray("spots").toJavaList(JSONObject.class);
                        if (listSpots.size() != 0) {
                            //添加测点
                            spots.addAll(listSpots);
                        }
                    }
                }
            }
        }
        if (devices.size() != 0) dataConfigMapper.addDevices(devices);
        if (spots.size() != 0) dataConfigMapper.addSpots(spots);
    }

    @Override
    public void initializeSafeTime() {
        //获取当前银行id、名称
        String bankId = UserContext.getUserBankId();
        String bankName = "";
        List<JSONObject> bank = dataConfigMapper.findBankByBankId(bankId);
        if (bank.size() != 0) bankName = bank.get(0).getString("bankName");
        //查询运行时间表，没有当前银行的数据新增，有则覆盖
        JSONObject runTime = dataConfigMapper.findRunTimeByBankId(bankId);
        if (runTime == null) {
            dataConfigMapper.insertSafeTime(bankId, bankName);
        } else {
            dataConfigMapper.updateSafeTime(bankId, bankName);
        }
    }

    @Override
    public JSONObject findDeviceStatus(String bankId) {
        //组装入参
        JSONObject param = new JSONObject();
        JSONObject terms1 = new JSONObject();
        List<QueryCondition> list1 = new ArrayList<>();
        list1.add(new QueryCondition("event_location", "like", new String[]{"%/" + bankId + "/%"}));
        list1.add(new QueryCondition("is_recover", "eq", 0));
        list1.add(new QueryCondition("is_confirm", "eq", 0));
        list1.add(new QueryCondition("event_level", "lte", 10));
        terms1.put("terms", list1);

        JSONObject terms2 = new JSONObject();
        List<QueryCondition> list2 = new ArrayList<>();
        list2.add(new QueryCondition("event_location", "like", new String[]{"%/" + bankId}));
        list2.add(new QueryCondition("is_recover", "eq", 0));
        list2.add(new QueryCondition("is_confirm", "eq", 0));
        list2.add(new QueryCondition("event_level", "lte", 10));
        terms2.put("terms", list2);

        List<JSONObject> where = new LinkedList<>();
        where.add(terms1);
        where.add(terms2);
        param.put("group", "event_location");
        param.put("where", where);
        String account = UserContext.getUserAccount();
        String countStr = eventRpc.getEventLastCount(AlarmParamUtils.createCookie(account), param);
        JSONObject jsonObject = JSONObject.parseObject(countStr);
        return jsonObject.getJSONObject("data");
    }

    @Override
    public List<JSONObject> userList(String word) {
        List<JSONObject> list = dataConfigMapper.userList(word);
        list.forEach(a -> {
            a.put("formMessage", a.getJSONArray("formMessage"));
            a.put("formEmail", a.getJSONArray("formEmail"));
        });
        return list;
    }

    @Override
    public List<JSONObject> findEmployee() {
        List<JSONObject> res = new LinkedList<>();
        String str1 = eventRpc.getEmployeeList(InfrasConstant.KE_RPC_COOKIE);
        JSONObject json = JSONObject.parseObject(str1);
        if ("00".equals(json.getString("error_code"))) {
            JSONArray jsonArray = json.getJSONObject("data").getJSONArray("employees");
            if (!jsonArray.isEmpty()) res = jsonArray.toJavaList(JSONObject.class);
        }
        return res;
    }

    @Override
    public void addUser(List<JSONObject> list) {
        for (JSONObject j : list) {
            j.put("roles", j.getString("roles"));
            j.put("departments", j.getString("departments"));
        }
        dataConfigMapper.addUserByList(list);
    }

    @Override
    public void deleteUser(int id) {
        dataConfigMapper.deleteUser(id);
    }

    @Override
    public List<JSONObject> findFormList() throws Exception {
        String cookie = RpcUtil.getCookie();
        String result = flowFormRpc.getFormList(cookie);
        JSONObject json = JSONObject.parseObject(result);
        if (!"200".equals(json.getString("status")))
            throw new Exception("查询流程引擎失败，接口：/api/flow/api/v1/bfm/config/data/module/list，返回信息：" + json.toJSONString());
        List<JSONObject> data = JSONArray.parseArray(json.getString("data")).toJavaList(JSONObject.class);
        return data;
    }

    @Override
    public void updateUser(JSONObject param) {
        dataConfigMapper.updateUser(param.getIntValue("id"), param.getString("alarmMessage"), param.getString("alarmEmail"),
                param.getIntValue("startFormMessage"), param.getJSONArray("formMessage").toJSONString(),
                param.getIntValue("startFormEmail"), param.getJSONArray("formEmail").toJSONString());
    }

    @Override
    public void mapOtherUser(JSONObject param) {
        JSONArray array = param.getJSONArray("list");
        if (!array.isEmpty()) {
            List<JSONObject> list = array.toJavaList(JSONObject.class);
            JSONObject config = param.getJSONObject("config");
            config.put("formMessage",config.getJSONArray("formMessage").toJSONString());
            config.put("formEmail",config.getJSONArray("formEmail").toJSONString());
            for (JSONObject j : list) {
                j.put("roles", j.getString("roles"));
                j.put("departments", j.getString("departments"));
                j.putAll(config);
            }
            dataConfigMapper.replaceUserByList(list);
        }
    }

    @Override
    public void informFormMessage(String moduleKey, String title, String context) {
        log.info("informFormMessage：moduleKey:{},title:{},context:{}", moduleKey, title, context);
        if (YmlConfig.loginTest != null && !"true".equals(YmlConfig.loginTest)) {
            //获取需要发送代办消息的用户
            List<String> touser = dataConfigMapper.findUserNeedInform(moduleKey, "formMessage");
            //发送代办消息
            Object requestJson = BusinessUtil.createParamForSendMessage(title + "，" + context, touser);
            log.info("移动OA消息推送，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson);
            String token = BusinessUtil.getToken();
            String str = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson);
            log.info("移动OA消息推送，出参：{}", str);

            //获取需要发送邮件的用户
            List<String> touser2 = dataConfigMapper.findUserNeedInform(moduleKey, "formEmail");
            //发送邮件
            Object requestJson2 = BusinessUtil.createParamForSendEmail(title + "，" + context, touser2);
            log.info("移动OA发送邮件，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson2);
            String str2 = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson2);
            log.info("移动OA发送邮件，出参：{}", str2);
        }
    }
}
