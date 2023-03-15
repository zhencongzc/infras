package com.cmbc.infras.system.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.*;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.event.CountDoneResult;
import com.cmbc.infras.dto.event.SubBankGroup;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.dto.ops.*;
import com.cmbc.infras.dto.rpc.Monitor;
import com.cmbc.infras.dto.rpc.MonitorResult;
import com.cmbc.infras.dto.rpc.MonitorRpcParam;
import com.cmbc.infras.dto.rpc.ResourceItem;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DeviceSpotMapper;
import com.cmbc.infras.system.rpc.*;
import com.cmbc.infras.system.service.BankService;
import com.cmbc.infras.system.service.HeadMainService;
import com.cmbc.infras.system.service.HistoryAlarmService;
import com.cmbc.infras.system.service.MonitorService;
import com.cmbc.infras.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class HeadMainServiceImpl implements HeadMainService {

    Logger LOG = LoggerFactory.getLogger("Radar");

    @Resource
    private BankService bankService;
    @Resource
    private MonitorService monitorService;
    @Resource
    private HistoryAlarmService historyAlarmService;

    @Resource
    private BankMapper bankMapper;
    @Resource
    private DeviceSpotMapper deviceSpotMapper;

    @Resource
    private HeartBeatRpc heartBeatRpc;
    @Resource
    private FlowFormRpc flowFormRpc;
    @Resource
    private EventRpc eventRpc;
    @Resource
    private HealthRpc healthRpc;

    @Resource
    private ExecutorService cachedThreadPool;

    @Override
    public BaseResult<SiteStatis> getSiteStatis() throws Exception {
        //设置互斥锁5分钟，成功的话开启线程更新缓存
        if (DataRedisUtil.addStringToRedisByExpireTime("site_statis_mutex", "1", 1000 * 60 * 5l) != null) {
            cachedThreadPool.execute(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    log.info("站点统计siteStatis开始更新缓存...");
                    long l = System.currentTimeMillis();
                    SiteStatis site = new SiteStatis();
                    //分别查询4个等级的银行的心跳
                    //分行
                    List<Bank> level1Banks = bankMapper.getBanksByLevelLink(1);
                    int level1 = getHeartBeat(level1Banks);
                    site.setLevel1Total(level1Banks.size());
                    site.setLevel1TotalOn(level1);
                    //二级分行
                    List<Bank> level2Banks = bankMapper.getBanksByLevelLink(2);
                    int level2 = getBankHeartBeat(level2Banks);
                    site.setLevel2Total(level2Banks.size());
                    site.setLevel2TotalOn(level2);
                    //支行
                    List<Bank> level3Banks = bankMapper.getBanksByLevelLink(3);
                    int level3 = getBankHeartBeat(level3Banks);
                    site.setLevel3Total(level3Banks.size());
                    site.setLevel3TotalOn(level3);
                    //村镇银行
                    List<Bank> level4Banks = bankMapper.getBanksByLevelLink(4);
                    int level4 = getBankHeartBeat(level4Banks);
                    site.setLevel4Total(level4Banks.size());
                    site.setLevel4TotalOn(level4);
                    DataRedisUtil.addStringToRedis("site_statis", JSON.toJSONString(site));
                    log.info("站点统计siteStatis更新缓存成功！耗时：{}ms", System.currentTimeMillis() - l);
                }
            });
        }
        SiteStatis site = DataRedisUtil.getStringFromRedis("site_statis", SiteStatis.class);
        return BaseResult.success(site);
    }

    private int getHeartBeat(List<Bank> banks) throws Exception {
        AtomicInteger res = new AtomicInteger();
        List<ResourceItem> resources = new LinkedList<>();
        for (Bank bank : banks) {
            resources.add(new ResourceItem(bank.getLinkId()));
        }
        MonitorRpcParam param = new MonitorRpcParam(resources);
        String resultStr = heartBeatRpc.getHeartBeat(InfrasConstant.KE_RPC_COOKIE, param);
        JSONObject obj = JSONObject.parseObject(resultStr);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) throw new Exception("查询KE工程组态-连接视图报错");
        String data = obj.getString("data");
        MonitorResult monitorResult = JSON.parseObject(data, MonitorResult.class);
        monitorResult.getResources().forEach((a) -> {
            if (a.getStatus() == 1) res.getAndIncrement();
        });
        return res.get();
    }

    //二级分行、支行、村镇银行需先拿到旗下的所有设备id，根据设备状态判断站点状态，只要有一个设备在线便认定该站点在线
    private int getBankHeartBeat(List<Bank> banks) throws Exception {
        int onCount = 0;
        for (Bank bank : banks) {
            int deviceHeartBeat = 0;
            String linkId = bank.getLinkId();
            if (linkId == null || "".equals(linkId)) {
                continue;
            }
            //通过连接视图id获取银行站点连接视图下的设备数据
            String str = eventRpc.spaceView(InfrasConstant.KE_RPC_COOKIE, linkId, "ci_type", "2,3,5,6,7", "5"
                    , "list");
            JSONArray array = JSONArray.parseArray(str);
            ArrayList<String> linkIdList = new ArrayList<>();
            for (int i = 1; i < array.size(); i++) {
                JSONObject jsonObject = (JSONObject) array.get(i);
                String deviceResourceId = jsonObject.getString("resource_id");
                linkIdList.add(deviceResourceId);
            }
            deviceHeartBeat = getDeviceHeartBeat(linkIdList);
            if (deviceHeartBeat != 0) {
                onCount++;
            }
        }
        return onCount;
    }

    private int getDeviceHeartBeat(List<String> linkIdList) throws Exception {
        AtomicInteger res = new AtomicInteger();
        List<ResourceItem> resources = new LinkedList<>();
        for (String linkId : linkIdList) {
            resources.add(new ResourceItem(linkId));
        }
        MonitorRpcParam param = new MonitorRpcParam(resources);
        String resultStr = heartBeatRpc.getHeartBeat(InfrasConstant.KE_RPC_COOKIE, param);
        JSONObject obj = JSONObject.parseObject(resultStr);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) throw new Exception("查询设备KE工程组态-连接视图报错");
        String data = obj.getString("data");
        MonitorResult monitorResult = JSON.parseObject(data, MonitorResult.class);
        monitorResult.getResources().forEach((a) -> {
            if (a.getStatus() == 1) res.getAndIncrement();
        });
        return res.get();
    }

    /**
     * 组装雷达显示数据-循环一级40家分行
     */
    private AlarmRadar getRadar(SubBankGroup group, String keAccount) {
        AlarmRadar radar = new AlarmRadar();
        radar.setBankId(group.getBankId());
        radar.setBankName(group.getBankName());
        //一级分行机房
        LOG.info("一级分行机房getRadar bankId:" + group.getBankId() + ",account:" + keAccount);
        CountDoneResult result = historyAlarmService.countDone(Arrays.asList(group.getBankId()), keAccount);
        LOG.info("一级分行机房getRadar result:" + JSON.toJSONString(result));
        //branch-本机房
        radar.setBranch(result.getCount().getCount());
        radar.setBranchDone(result.getCountDone().getCount());
        //二级支行
        if (group.getBranchs() != null && group.getBranchs().size() > 0) {
            LOG.info("二级支行getRadar bankIds:" + JSON.toJSONString(group.getBranchs()) + ",account:" + keAccount);
            CountDoneResult branchCdr = historyAlarmService.countDone(group.getBranchs(), keAccount);
            LOG.info("二级支行getRadar branchCdr:" + JSON.toJSONString(branchCdr));
            //branch2二级分行
            radar.setBranch2(branchCdr.getCount().getCount());
            radar.setBranch2Done(branchCdr.getCountDone().getCount());
        }
        //三级支行
        if (group.getSubs() != null && group.getSubs().size() > 0) {
            LOG.info("三级支行getRadar bankIds:" + JSON.toJSONString(group.getSubs()) + ",account:" + keAccount);
            result = historyAlarmService.countDone(group.getSubs(), keAccount);
            LOG.info("三级支行getRadar result:" + JSON.toJSONString(result));
            //subs三级支行
            radar.setSub(result.getCount().getCount());
            radar.setSubDone(result.getCountDone().getCount());
        }
        //四级村镇
        if (group.getTowns() != null && group.getTowns().size() > 0) {
            LOG.info("四级村镇getRadar bankIds:" + JSON.toJSONString(group.getTowns()) + ",account:" + keAccount);
            result = historyAlarmService.countDone(group.getTowns(), keAccount);
            LOG.info("四级村镇getRadar result:" + JSON.toJSONString(result));
            //四级村镇
            radar.setTown(result.getCount().getCount());
            radar.setTownDone(result.getCountDone().getCount());
        }
        radar.sum();
        return radar;
    }

    @Override
    public BaseResult<AlarmRadar> getBankAlarmRadar(String bankId) {
        String account = UserContext.getUserAccount();
        Bank bank = bankService.getBankById(bankId);
        SubBankGroup group = bankService.getSubBanksGroup(bankId);
        group.setBankId(bank.getBankId());
        group.setBankName(bank.getBankName());
        AlarmRadar radar = getRadar(group, account);
        return BaseResult.success(radar);
    }

    @Override
    public BaseResult<List<Evaluate>> getEvaluate() {
        JSONObject param = new JSONObject();
        param.put("account", "admin");
        param.put("modelId", YmlConfig.evaluateFormId);
        String str = healthRpc.evaluation(param);
//        log.info("综合评价接口/head/evaluates:url:{},请求body:{},返回body:{}", YmlConfig.keUrl + "/health/common/evaluation", param.toJSONString(), str);
        JSONObject result = JSONObject.parseObject(str);
        List<Evaluate> items = null;
        if (result.getIntValue("state") == 200) items = result.getJSONArray("data").toJavaList(Evaluate.class);
        return BaseResult.success(items, items.size());
    }

    /**
     * 机房温湿度-总行
     */
    @Override
    public BaseResult<List<Humiture>> getHumiture() {
        String account = UserContext.getUserAccount();
        String bankId = UserContext.getUserBankId();
        if (!"0".equals(bankId)) {
            log.error("总行查询机房温湿度,用户{}, bankId:{},错误", account, bankId);
        }
        //所有测点集合-resourceIds
        SpotIds spotIds = new SpotIds();
        //总行只查40家分行
        List<Bank> banks = bankMapper.getBanksByLevel(1);
        for (Bank bank : banks) {
            setBankResource(bank, spotIds, DeviceTypeEnum.HUM.getType());
        }
        Map<String, Monitor> moMap = new HashMap<>();
        List<Monitor> monitors = monitorService.getMonitorList(spotIds.getResourceIds());
        for (Monitor mo : monitors) {
            moMap.put(mo.getResource_id(), mo);
        }
        List<Humiture> hums = new ArrayList<>();
        for (Bank bank : banks) {
            Humiture hum = getBankHumiture(bank, moMap);
            hums.add(hum);
        }
        return BaseResult.success(hums, hums.size());
    }

    private Humiture getBankHumiture(Bank bank, Map<String, Monitor> moMap) {
        Humiture hum = new Humiture();
        hum.setBankId(bank.getBankId());
        hum.setBankName(bank.getBankName());
        List<Float> tempers = new ArrayList<>();
        List<Float> humidis = new ArrayList<>();
        List<Device> ds = bank.getDevices();
        if (ds.isEmpty()) {
            return hum.initEmpty();
        }
        for (Device d : ds) {
            List<Spot> ss = d.getSpots();
            if (ss.isEmpty()) {
                return hum;
            }
            for (Spot s : ss) {
                Monitor mo = moMap.get(s.getSpotId());
                if (mo != null) {
                    String v = mo.getReal_value();
                    switch (s.getSpotType()) {
                        case DevSpotConstant.SPOT_HUM_TEMP:
                            tempers.add(NumberUtils.parseFloat(v));
                            break;
                        case DevSpotConstant.SPOT_HUM_HUMI:
                            humidis.add(NumberUtils.parseFloat(v));
                            break;
                        default:
                    }
                }
            }
        }
        if (!tempers.isEmpty()) {
            Collections.sort(tempers);
            hum.setMinTemper(String.valueOf(tempers.get(0)));
            hum.setMaxTemper(String.valueOf(tempers.get(tempers.size() - 1)));
        }
        if (!humidis.isEmpty()) {
            Collections.sort(humidis);
            hum.setMinHumidity(String.valueOf(humidis.get(0)));
            hum.setMaxHumidity(String.valueOf(humidis.get(humidis.size() - 1)));
        }
        log.info("hum:{}, tempers:{},humidis:{}", JSON.toJSONString(hum), JSON.toJSONString(tempers), JSON.toJSONString(humidis));
        return hum;
    }

    /**
     * 设置银行资源
     */
    private void setBankResource(Bank bank, SpotIds spotIds, int type) {
        Map<String, Object> param = new HashMap<>();
        param.put("bankId", bank.getBankId());
        param.put("deviceType", type);
        List<Device> devs = deviceSpotMapper.getDevices(param);
        bank.setDevices(devs);
        for (Device dev : devs) {
            List<Spot> spots = deviceSpotMapper.getDevSpots(dev);
            dev.setSpots(spots);
            spotIds.addAll(spots);
        }
    }

    @Override
    public BaseResult<List<JSONObject>> getPartol() {
        List<JSONObject> partols = new ArrayList<>();
        String startTime = DateTimeUtils.getTodayZeroFormat("yyyy-MM-dd HH:mm:ss");
        String endTime = DateTimeUtils.getCurrentFormat("yyyy-MM-dd HH:mm:ss");
        //组建查询参数
        JSONObject param = FLowFormParamUtil.createFormParam(YmlConfig.partolFormId, startTime, endTime);
        //查询巡检动态所有表单状态并组装
        String cookie = RpcUtil.getCookie();
        String res = flowFormRpc.getFormState(cookie, "OnSiteInspectionTask", "status");
        JSONObject json = JSONObject.parseObject(res);
        if (!"200".equals(json.getString("status"))) return BaseResult.fail("获取表单状态失败");
        JSONObject stat = new JSONObject();
        List<JSONObject> data = JSONArray.parseArray(json.getString("data")).toJavaList(JSONObject.class);
        JSONArray label = new JSONArray();
        StringBuffer value = new StringBuffer();
        for (JSONObject d : data) {
            label.add(d.getString("label"));
            value.append(d.getString("value")).append(",");
        }
        if (value.length() > 0) value = value.deleteCharAt(value.length() - 1);
        stat.put("label", label);
        stat.put("value", value.toString());
        param.getJSONObject("query").getJSONObject("search").put("status", stat);
        //查询巡检动态数据
        String token = UserContext.getAuthToken();
        String rst = flowFormRpc.getFormData(token, param);
        JSONObject result = JSONObject.parseObject(rst);
        if (result.getIntValue("status") != 200) return BaseResult.fail("查询巡检动态数据出错！");
        List<JSONObject> list = result.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
        for (JSONObject obj : list) {
            JSONObject j = new JSONObject();
            j.put("bankName", obj.getString("Field_xxx_create_dept"));
            j.put("taskName", obj.getString("Field_xxx_title"));
            j.put("startTime", obj.getString("plan_start_time"));
            j.put("operator", obj.getString("Field_xxx_creator"));
            j.put("status", obj.getString("row_status"));
            j.put("stateName", obj.getString("status"));
            partols.add(j);
        }
        return BaseResult.success(partols, partols.size());
    }

    @Override
    public BaseResult<List<JSONObject>> getWorkCheck() {
        String token = UserContext.getAuthToken();
        String year = DateTimeUtils.getCurrentFormat("yyyy");
        String startTime = year + "-01-01 00:00:00";
        String endTime = year + "-12-31 23:59:59";
        //查询维护任务数据
        List<JSONObject> maintains = new ArrayList<>();
        JSONObject param = FLowFormParamUtil.createFormParam(YmlConfig.maintainFormId, startTime, endTime);
        String rst = flowFormRpc.getFormData(token, param);
        JSONObject result = JSONObject.parseObject(rst);
        if (result.getIntValue("status") == 200) {
            List<JSONObject> list = result.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
            for (JSONObject obj : list) {
                JSONObject j = new JSONObject();
                j.put("bankName", obj.getString("Field_xxx_create_dept"));
                j.put("taskName", obj.getString("Field_xxx_title"));
                j.put("planTime", obj.getString("plan_start_time"));
                j.put("startTime", obj.getString("execute_time"));
                j.put("operator", obj.getString("Field_xxx_creator"));
                j.put("status", obj.getString("row_status"));
                j.put("stateName", obj.getString("status"));
                maintains.add(j);
            }
        }
        //查询演练任务数据
        List<JSONObject> deduces = new ArrayList<>();
        JSONObject param1 = FLowFormParamUtil.createFormParam(YmlConfig.deduceFormId, startTime, endTime);
        String rst1 = flowFormRpc.getFormData(token, param1);
        JSONObject result1 = JSONObject.parseObject(rst1);
        if (result1.getIntValue("status") == 200) {
            List<JSONObject> list = result1.getJSONObject("data").getJSONArray("instancesData").toJavaList(JSONObject.class);
            for (JSONObject obj : list) {
                JSONObject j = new JSONObject();
                j.put("bankName", obj.getString("Field_xxx_create_dept"));
                j.put("taskName", obj.getString("Field_xxx_title"));
                j.put("planTime", obj.getString("plan_start_time"));
                j.put("startTime", obj.getString("execute_time"));
                j.put("operator", obj.getString("Field_xxx_creator"));
                j.put("status", obj.getString("row_status"));
                j.put("stateName", obj.getString("status"));
                deduces.add(j);
            }
        }
        maintains.addAll(deduces);
        return BaseResult.success(maintains, maintains.size());
    }

}