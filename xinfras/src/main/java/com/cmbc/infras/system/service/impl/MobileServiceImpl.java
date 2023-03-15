package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.DevSpotConstant;
import com.cmbc.infras.constant.DeviceTypeEnum;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.constant.SpotTypeEnum;
import com.cmbc.infras.dto.*;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.EventInfo;
import com.cmbc.infras.dto.health.AlarmRequestParam;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.dto.monitor.UpsInfo;
import com.cmbc.infras.dto.rpc.Monitor;
import com.cmbc.infras.dto.rpc.MonitorResult;
import com.cmbc.infras.dto.rpc.MonitorRpcParam;
import com.cmbc.infras.dto.rpc.ResourceItem;
import com.cmbc.infras.dto.rpc.event.*;
import com.cmbc.infras.system.controller.AlarmController;
import com.cmbc.infras.system.exception.DataErrorException;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DeviceSpotMapper;
import com.cmbc.infras.system.mapper.MonitorMapper;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.rpc.FlowFormRpc;
import com.cmbc.infras.system.rpc.MonitorRpc;
import com.cmbc.infras.system.service.*;
import com.cmbc.infras.system.util.TransferUtil;
import com.cmbc.infras.util.AlarmParamUtils;
import com.cmbc.infras.util.EventParamUtils;
import com.cmbc.infras.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class MobileServiceImpl implements MobileService {

    @Resource
    private AlarmController alarmController;
    @Resource
    private LabelService labelService;
    @Resource
    private EventService eventService;
    @Resource
    private FlowFormService flowFormService;
    @Resource
    private AlarmService alarmService;

    @Resource
    private BankService bankService;

    @Resource
    private MonitorMapper monitorMapper;
    @Resource
    private BankMapper bankMapper;
    @Resource
    private DeviceSpotMapper deviceSpotMapper;

    @Resource
    private FlowFormRpc flowFormRpc;
    @Resource
    private MonitorRpc monitorRpc;
    @Resource
    private EventRpc eventRpc;

    /**
     * 参数bankId非空是跳转(总行,分行跳转)
     * bankId为空,则是登录账号查询
     * bankId=0,则查40家分行
     * bankId!=0,则查本行及所有下级
     */
    @Override
    public BaseResult<List<Bank>> getBanks(BaseParam param) {
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        Assert.hasLength(bankId, "无法获取银行ID");
        List<Bank> banks = new ArrayList<>();
        String sessionId = UserContext.getAuthToken();
        if (InfrasConstant.HEAD_BANK_ID.equals(bankId)) {
            banks = flowFormService.getBanksByLevel(1, sessionId);
        } else {
            //其它行账号,查询当前行及所有下级银行
            Bank bank = flowFormService.getBankById(bankId, sessionId);
            if (bank == null) {
                log.error("通过bankId取得银行信息返回空！bankId:{}", bankId);
            } else {
                banks.add(bank);
                List<Bank> subs = flowFormService.getSubBanks(bankId, sessionId);
                banks.addAll(subs);
            }
        }
        return BaseResult.success(banks, banks.size());
    }

    @Override
    public BaseResult<Boolean> confirm(AlarmConfirmParam param) {
        String account = UserContext.getUserAccount();
        param.setConfirm_by(account);
        Assert.state(param.getConfirm_type() > 0, "请选择告警分类");
        Assert.notEmpty(param.getEvent_list(), "受理数据不能为空！");
        Assert.hasLength(param.getConfirm_description(), "描述不能为空");
        BaseResult<Boolean> result = eventService.alarmConfirm(param);
        return result;
    }

    @Override
    public BaseResult<Boolean> accept(AlarmAcceptParam param) {
        String account = UserContext.getUserAccount();
        param.setAccept_by(account);
        Assert.notEmpty(param.getEvent_list(), "受理数据不能为空！");
        Assert.hasLength(param.getAccept_description(), "描述不能为空");
        BaseResult<Boolean> result = eventService.alarmAccept(param);
        return result;
    }

    @Override
    public BaseResult<List<AlarmEvent>> getAlarms(Label label) {
        String account = UserContext.getUserAccount();
        String sessionId = UserContext.getAuthToken();
        EventParam param = EventParamUtils.createEventParam(label);
        String resultStr = eventRpc.getEventLast(AlarmParamUtils.createCookie(account), param);
        List<Event> events = EventParamUtils.parseEventResult(resultStr);
        List<AlarmEvent> alarms = TransferUtil.eventsToAlarms(events);
        //添加bankId,bankName
        HashSet<String> ids = flowFormService.getAllBankIds(sessionId);
        Iterator<AlarmEvent> iterator = alarms.iterator();
        while (iterator.hasNext()) {
            AlarmEvent alarm = iterator.next();
            String location = alarm.getEventLocation(); //project_root/0_931/0_969/0_1083/0_1183
            String[] arr = location.split("/");  //位置id
            String rid = arr.length > 2 ? arr[2] : "-1";
            if (ids.contains(rid)) {
                Bank bank = flowFormService.getBankById(rid, sessionId);
                alarm.setBankId(bank.getBankId());
                alarm.setBankName(bank.getBankName());
            } else {
                //未查到银行id，可能是分行的灾备id，去掉该告警
                iterator.remove();
            }
        }
        return BaseResult.success(alarms, alarms.size());
    }

    @Override
    public List<Bank> getSubBanks() {
        //查询当前及所有下级分行，总行除外
        String bankId = UserContext.getUserBankId();
        List<Bank> subBanks = bankMapper.getSubBanksById(bankId);
        if (!InfrasConstant.HEAD_BANK_ID.equals(bankId)) subBanks.add(bankService.getBankById(bankId));
        return subBanks;
    }

    @Override
    public BaseResult<List<JSONObject>> upss(List<Bank> subBanks) throws Exception {
        long start1 = System.currentTimeMillis();
        log.info("upss-test:开始执行upss...");

        List<JSONObject> res = new ArrayList<>();
        //查询所有设备
        List<String> bankIds = new ArrayList<>();
        for (Bank bk : subBanks) {
            bankIds.add(bk.getBankId());
        }
        Map<String, Object> param = new HashMap<>();
        param.put("bankIds", bankIds);
        param.put("deviceType", DeviceTypeEnum.UPS.getType());
        List<Device> devices;
        devices = deviceSpotMapper.getBanksDevices(param);
        if (devices.size() == 0) return BaseResult.success(res);

        //查到设备测点
        List<String> devIds = new ArrayList<>();
        List<UpsInfo> upss = new ArrayList<>();
        Map<String, UpsInfo> upsMap = new HashMap<>();
        for (Device dev : devices) {
            UpsInfo ups = new UpsInfo(dev);
            upss.add(ups);
            devIds.add(dev.getDeviceId());
            upsMap.put(dev.getDeviceId(), ups);
        }
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        if (spots.size() == 0) {
            for (UpsInfo u : upss) {
                res.add((JSONObject) JSONObject.toJSON(u));
            }
            return BaseResult.success(res);
        }

        long t1 = System.currentTimeMillis();
        log.info("upss-test:第1段执行时间：" + (t1 - start1));

        //查询测点数据
        Map<String, Spot> spotMap = new HashMap<>();
        List<String> spotIds = new ArrayList<>();
        for (Spot spot : spots) {
            spotIds.add(spot.getSpotId());
            spotMap.put(spot.getSpotId(), spot);
        }
        List<ResourceItem> resources = new ArrayList<>();
        for (String leafId : spotIds) {
            resources.add(new ResourceItem(leafId));
        }
        MonitorRpcParam param1 = new MonitorRpcParam(resources);
        String resultStr = monitorRpc.getMonitorList(InfrasConstant.KE_RPC_COOKIE, param1);
        JSONObject obj = JSONObject.parseObject(resultStr);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) throw new DataErrorException("查询测点数据失败！");
        String data = obj.getString("data");
        MonitorResult monitorResult = JSON.parseObject(data, MonitorResult.class);
        List<Monitor> ms = monitorResult.getResources();
        if (ms.size() == 0) return BaseResult.fail("没有查到测点数据！");

        long t2 = System.currentTimeMillis();
        log.info("upss-test:第2段执行时间：" + (t2 - t1));

        //封装ups的功率、负载率和后备时间
        List<String> alarmResourceIds = new ArrayList<>();//存放告警测点的resourceId
        BaseResult<List<EventInfo>> eventResult = eventService.events(null);
        if (!eventResult.isSuccess()) return BaseResult.fail(eventResult.getMessage());
        List<EventInfo> einfos = eventResult.getData();
        for (EventInfo ei : einfos) {
            alarmResourceIds.add(ei.getResourceId());
        }
        for (Monitor mo : ms) {
            Spot spot = spotMap.get(mo.getResource_id());
            UpsInfo ups = upsMap.get(spot.getDeviceId());
            switch (spot.getSpotType()) {
                case DevSpotConstant.SPOT_UPS_POWER:
                    ups.setPower(mo.getReal_value());
                    break;
                case DevSpotConstant.SPOT_UPS_LOAD:
                    ups.setLoadRate(mo.getReal_value());
                    break;
                case DevSpotConstant.SPOT_UPS_TIME:
                    ups.setBackTime(mo.getReal_value());
                    break;
                default:
            }
        }

        //先过滤：分行展示A分、B分；二级分行、支行、村镇银行展示A总、B总
        Iterator<UpsInfo> iterator = upss.iterator();
        while (iterator.hasNext()) {
            UpsInfo next = iterator.next();
            String groupName = next.getGroupName();
            int bankLevel = next.getBankLevel();
            if (bankLevel == 1) {
                if (!"A分".equals(groupName) && !"B分".equals(groupName)) iterator.remove();
            } else {
                if (!"A总".equals(groupName) && !"B总".equals(groupName)) iterator.remove();
            }
        }

        long t3 = System.currentTimeMillis();
        log.info("upss-test:第3段执行时间：" + (t3 - t2));

        //查询UPS真实设备通信状态，异常时高亮
        List<ResourceItem> resources1 = new ArrayList<>();
        for (UpsInfo u : upss) {
            String trueDeviceId = u.getTrueDeviceId();
            if (StringUtils.isNotBlank(trueDeviceId)) resources1.add(new ResourceItem(trueDeviceId));
        }
        MonitorRpcParam param2 = new MonitorRpcParam(resources1);
        String resultStr1 = monitorRpc.getMonitorList(InfrasConstant.KE_RPC_COOKIE, param2);
        JSONObject obj1 = JSONObject.parseObject(resultStr1);
        String code1 = obj1.getString("error_code");
        if (!"00".equals(code1)) return BaseResult.fail("查询Ke工程组态-设备通信状态失败！");
        String data1 = obj1.getString("data");
        MonitorResult monitorResult1 = JSON.parseObject(data1, MonitorResult.class);
        List<Monitor> resource = monitorResult1.getResources();
        HashMap<String, Integer> map = new HashMap<>();
        resource.forEach((a) -> map.put(a.getResource_id(), a.getStatus()));
        for (UpsInfo u : upss) {
            String trueDeviceId = u.getTrueDeviceId();
            if (StringUtils.isNotBlank(trueDeviceId)) u.setAlarm(map.get(trueDeviceId) == 1 ? 0 : 1);
        }

        long t4 = System.currentTimeMillis();
        log.info("upss-test:第4段执行时间：" + (t4 - t3));
        log.info("upss-test:执行结束，总耗时：：" + (t4 - start1));

        //封装数据
        upss.forEach(a -> res.add((JSONObject) JSONObject.toJSON(a)));
        return BaseResult.success(res, res.size());
    }

    @Override
    public List<JSONObject> getUpss(String bankId) throws Exception {
        List<Bank> subBanks = new LinkedList<>();
        subBanks.add(bankService.getBankById(bankId));
        return upss(subBanks).getData();
    }

    @Override
    public HashMap<String, Integer> alarmCount(JSONObject json) {
        //根据标签，获取当日5000条告警
        Label label = new Label();
        label.setEventLevel(json.getString("eventLevel"));
        label.setRecoverState(json.getString("recoverState"));//恢复状态
        label.setProcessState(json.getString("processState"));//处理状态
        EventGroupParam param = EventParamUtils.creatLastAlarmParam(label);
        param.getPage().setSize(5000);
        String resultStr = eventRpc.getEventLast(AlarmParamUtils.createCookie("admin"), param);
        List<Event> events = EventParamUtils.parseEventResult(resultStr);

        //按照bankId分组
        HashMap<String, Integer> map = new HashMap<>();
        List<Bank> banks = json.getJSONArray("list").toJavaList(Bank.class);
        banks.forEach(a -> map.put(a.getBankId(), 0));
        events.forEach(a -> {
            String[] arr = a.getEvent_location().split("/");
            String bankId = arr.length > 2 ? arr[2] : "-1";
            if (map.containsKey(bankId)) map.put(bankId, map.get(bankId) + 1);
        });

        return map;
    }
}
