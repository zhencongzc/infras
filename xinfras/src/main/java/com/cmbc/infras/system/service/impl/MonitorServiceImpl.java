package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.DevSpotConstant;
import com.cmbc.infras.constant.DeviceTypeEnum;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.constant.SpotTypeEnum;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.event.EventInfo;
import com.cmbc.infras.dto.health.AlarmRequestParam;
import com.cmbc.infras.dto.health.FormRequestParam;
import com.cmbc.infras.dto.monitor.*;
import com.cmbc.infras.dto.rpc.Monitor;
import com.cmbc.infras.dto.rpc.MonitorResult;
import com.cmbc.infras.dto.rpc.MonitorRpcParam;
import com.cmbc.infras.dto.rpc.ResourceItem;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.dto.rpc.event.EventParam;
import com.cmbc.infras.system.exception.DataErrorException;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DeviceSpotMapper;
import com.cmbc.infras.system.mapper.MonitorMapper;
import com.cmbc.infras.system.rpc.FlowFormRpc;
import com.cmbc.infras.system.rpc.MonitorRpc;
import com.cmbc.infras.system.service.BankService;
import com.cmbc.infras.system.service.EventService;
import com.cmbc.infras.system.service.MonitorService;
import com.cmbc.infras.util.EventParamUtils;
import com.cmbc.infras.util.NumberUtils;
import com.cmbc.infras.util.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class MonitorServiceImpl implements MonitorService {

    @Resource
    private EventService eventService;
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

    @Override
    public BaseResult<List<UpsInfo>> getUpss(String bankId) throws Exception {
        List<UpsInfo> upss = new ArrayList<>();
        List<String> devIds = new ArrayList<>();
        List<Device> devices = getBankDevice(bankId, DeviceTypeEnum.UPS.getType());
        if (devices.size() == 0) return BaseResult.success(upss);
        //查到设备逻辑
        Map<String, UpsInfo> upsMap = new HashMap<>();
        for (Device dev : devices) {
            UpsInfo ups = new UpsInfo(dev);
            upss.add(ups);
            devIds.add(dev.getDeviceId());
            upsMap.put(dev.getDeviceId(), ups);
        }
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        if (spots.size() == 0) return BaseResult.success(upss);
        //查到测点逻辑
        Map<String, Spot> spotMap = new HashMap<>();
        List<String> spotIds = new ArrayList<>();
        for (Spot spot : spots) {
            spotIds.add(spot.getSpotId());
            spotMap.put(spot.getSpotId(), spot);
        }
        List<Monitor> ms = getMonitorList(spotIds);
        if (ms.size() == 0) return BaseResult.fail("没有查到测点数据！");
        //查到测点数据逻辑
        List<String> alarmResourceIds = new ArrayList<>();//存放告警测点的resourceId
        BaseResult<List<EventInfo>> eventResult = eventService.events(bankId);
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
        //更新电池剩余时间 逻辑：根据设备的真实设备id查数据库中“电池放电”测点，查询对应的告警并计算充放电时间
        for (UpsInfo u : upss) {
            String deviceId = u.getTrueDeviceId();//真实设备id
            if (StringUtils.isNotBlank(deviceId)) {
                long dischargeTime = 0;//放电时间 单位秒
                long chargeTime = 0;//充电时间 单位秒
                String spotId = "";//电池放电测点id
                //从数据库查“电池放电”测点
                spotId = deviceSpotMapper.findSpotByDeviceIdAndType(deviceId, SpotTypeEnum.UPS_DISCHARGE.getType());
                if (spotId != null) {
                    //查询告警信息（紧急告警，一天数据），匹配resource_id和spotId
                    List<JSONObject> listEvent = new LinkedList<>();
                    int[] eventLevel = new int[]{1};
                    long end = System.currentTimeMillis() / 1000;
                    long start = end - 60 * 60 * 24 * 1;//查询近一天的数据
                    String[] bankIdArr = new String[]{bankId};
                    AlarmRequestParam param2 = new AlarmRequestParam(bankIdArr, start, end, eventLevel);
                    String result1 = flowFormRpc.getAlarmData(InfrasConstant.KE_RPC_COOKIE, param2);
                    JSONObject json1 = JSONObject.parseObject(result1);
                    if (!"00".equals(json1.getString("error_code"))) throw new Exception("查询告警信息失败");
                    List<JSONObject> eventList = json1.getJSONObject("data").getJSONArray("event_list").toJavaList(JSONObject.class);
                    for (JSONObject e : eventList) {
                        if (e.getString("resource_id").equals(spotId)) listEvent.add(e);
                    }
                    //判断产生时间和恢复时间
                    if (listEvent.size() != 0) {
                        //只查看最新的告警
                        JSONObject firstEvent = listEvent.get(0);
                        long eventTime = firstEvent.getLongValue("event_time");
                        long recoverTime = firstEvent.getLongValue("recover_time");
                        //有恢复时间
                        if (recoverTime != 0) {
                            dischargeTime = recoverTime - eventTime;//放电时间等于恢复时间-产生时间
                            chargeTime = end - recoverTime;//恢复时间等于当前时间-恢复时间
                        } else {
                            //无恢复时间
                            dischargeTime = end - eventTime;//放电时间等于当前时间-产生时间
                        }
                    }
                    dischargeTime /= 60;//转换为min
                    chargeTime = chargeTime * 2 / 60;//转换为min，充电效率为放电的2倍
                    String backTime1 = u.getBackTime();
                    int backTime = (int) Double.parseDouble(backTime1 == null ? "0" : backTime1);//电池后备时间
                    //放电时间超过电池后备时间，剩余时间为0
                    long leftTime = backTime < dischargeTime ? 0 : backTime - dischargeTime;
                    //充电时间加剩余时间超过电池后备时间，剩余时间为电池后备时间
                    leftTime = backTime < leftTime + chargeTime ? backTime : leftTime + chargeTime;
                    leftTime = leftTime < 0 ? 0 : leftTime > backTime ? backTime : leftTime;
                    u.setBackTime(leftTime + "");
                }
            }
        }
        //查询UPS真实设备通信状态，异常时高亮
        List<ResourceItem> resources = new ArrayList<>();
        for (UpsInfo u : upss) {
            String trueDeviceId = u.getTrueDeviceId();
            if (StringUtils.isNotBlank(trueDeviceId)) resources.add(new ResourceItem(trueDeviceId));
        }
        MonitorRpcParam param = new MonitorRpcParam(resources);
        String resultStr = monitorRpc.getMonitorList(InfrasConstant.KE_RPC_COOKIE, param);
        JSONObject obj = JSONObject.parseObject(resultStr);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) return BaseResult.fail("查询Ke工程组态-设备通信状态失败！");
        String data = obj.getString("data");
        MonitorResult monitorResult = JSON.parseObject(data, MonitorResult.class);
        List<Monitor> resource = monitorResult.getResources();
        HashMap<String, Integer> map = new HashMap<>();
        resource.forEach((a) -> map.put(a.getResource_id(), a.getStatus()));
        for (UpsInfo u : upss) {
            String trueDeviceId = u.getTrueDeviceId();
            if (StringUtils.isNotBlank(trueDeviceId)) u.setAlarm(map.get(trueDeviceId) == 1 ? 0 : 1);
        }
        //除了“不展示”的设备都展示
        Iterator<UpsInfo> iterator = upss.iterator();
        while (iterator.hasNext()) {
            if ("不展示".equals(iterator.next().getGroupName())) iterator.remove();
        }
        return BaseResult.success(upss, upss.size());
    }

    @Override
    public BaseResult<JSONObject> upss(String bankId) throws Exception {
        List<UpsInfo> upss = new ArrayList<>();
        List<String> devIds = new ArrayList<>();
        List<Device> devices = getBankDevice(bankId, DeviceTypeEnum.UPS.getType());
        if (devices.size() == 0) return BaseResult.success(upss);
        //查到设备逻辑
        Map<String, UpsInfo> upsMap = new HashMap<>();
        for (Device dev : devices) {
            UpsInfo ups = new UpsInfo(dev);
            upss.add(ups);
            devIds.add(dev.getDeviceId());
            upsMap.put(dev.getDeviceId(), ups);
        }
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        if (spots.size() == 0) return BaseResult.success(upss);
        //查到测点逻辑
        Map<String, Spot> spotMap = new HashMap<>();
        List<String> spotIds = new ArrayList<>();
        for (Spot spot : spots) {
            spotIds.add(spot.getSpotId());
            spotMap.put(spot.getSpotId(), spot);
        }
        List<Monitor> ms = getMonitorList(spotIds);
        if (ms.size() == 0) return BaseResult.fail("没有查到测点数据！");
        //查到测点数据逻辑
        List<String> alarmResourceIds = new ArrayList<>();//存放告警测点的resourceId
        BaseResult<List<EventInfo>> eventResult = eventService.events(bankId);
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
        //更新电池剩余时间 逻辑：根据设备的真实设备id查数据库中“电池放电”测点，查询对应的告警并计算充放电时间
        for (UpsInfo u : upss) {
            String deviceId = u.getTrueDeviceId();//真实设备id
            if (StringUtils.isNotBlank(deviceId)) {
                long dischargeTime = 0;//放电时间 单位秒
                long chargeTime = 0;//充电时间 单位秒
                String spotId = "";//电池放电测点id
                //从数据库查“电池放电”测点
                spotId = deviceSpotMapper.findSpotByDeviceIdAndType(deviceId, SpotTypeEnum.UPS_DISCHARGE.getType());
                if (spotId != null) {
                    //查询告警信息（紧急告警，一天数据），匹配resource_id和spotId
                    List<JSONObject> listEvent = new LinkedList<>();
                    int[] eventLevel = new int[]{1};
                    long end = System.currentTimeMillis() / 1000;
                    long start = end - 60 * 60 * 24 * 1;//查询近一天的数据
                    String[] bankIdArr = new String[]{bankId};
                    AlarmRequestParam param2 = new AlarmRequestParam(bankIdArr, start, end, eventLevel);
                    String result1 = flowFormRpc.getAlarmData(InfrasConstant.KE_RPC_COOKIE, param2);
                    JSONObject json1 = JSONObject.parseObject(result1);
                    if (!"00".equals(json1.getString("error_code"))) throw new Exception("查询告警信息失败");
                    List<JSONObject> eventList = json1.getJSONObject("data").getJSONArray("event_list").toJavaList(JSONObject.class);
                    for (JSONObject e : eventList) {
                        if (e.getString("resource_id").equals(spotId)) listEvent.add(e);
                    }
                    //判断产生时间和恢复时间
                    if (listEvent.size() != 0) {
                        //只查看最新的告警
                        JSONObject firstEvent = listEvent.get(0);
                        long eventTime = firstEvent.getLongValue("event_time");
                        long recoverTime = firstEvent.getLongValue("recover_time");
                        //有恢复时间
                        if (recoverTime != 0) {
                            dischargeTime = recoverTime - eventTime;//放电时间=恢复时间-产生时间
                            chargeTime = end - recoverTime;//充电时间=当前时间-恢复时间
                        } else {
                            //无恢复时间
                            dischargeTime = end - eventTime;//放电时间=当前时间-产生时间
                        }
                    }
                    dischargeTime /= 60;//转换为min
                    chargeTime = chargeTime * 2 / 60;//转换为min，充电效率为放电的2倍
                    String backTime1 = u.getBackTime();
                    int backTime = (int) Double.parseDouble(backTime1 == null ? "0" : backTime1);//电池后备时间
                    //放电时间超过电池后备时间，剩余时间为0
                    long leftTime = backTime < dischargeTime ? 0 : backTime - dischargeTime;
                    //充电时间加剩余时间超过电池后备时间，剩余时间为电池后备时间
                    leftTime = backTime < leftTime + chargeTime ? backTime : leftTime + chargeTime;
                    leftTime = leftTime < 0 ? 0 : leftTime > backTime ? backTime : leftTime;
                    u.setBackTime(leftTime + "");
                }
            }
        }
        //查询UPS真实设备通信状态，异常时高亮
        List<ResourceItem> resources = new ArrayList<>();
        for (UpsInfo u : upss) {
            String trueDeviceId = u.getTrueDeviceId();
            if (StringUtils.isNotBlank(trueDeviceId)) resources.add(new ResourceItem(trueDeviceId));
        }
        MonitorRpcParam param = new MonitorRpcParam(resources);
        String resultStr = monitorRpc.getMonitorList(InfrasConstant.KE_RPC_COOKIE, param);
        JSONObject obj = JSONObject.parseObject(resultStr);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) return BaseResult.fail("查询Ke工程组态-设备通信状态失败！");
        String data = obj.getString("data");
        MonitorResult monitorResult = JSON.parseObject(data, MonitorResult.class);
        List<Monitor> resource = monitorResult.getResources();
        HashMap<String, Integer> map = new HashMap<>();
        resource.forEach((a) -> map.put(a.getResource_id(), a.getStatus()));
        for (UpsInfo u : upss) {
            String trueDeviceId = u.getTrueDeviceId();
            if (StringUtils.isNotBlank(trueDeviceId)) u.setAlarm(map.get(trueDeviceId) == 1 ? 0 : 1);
        }
        //分组返回数据
        JSONObject res = new JSONObject();//结果
        List<JSONObject> list = new LinkedList<>();//虚拟A路、B路设备
        JSONObject aRoad = new JSONObject();
        JSONObject bRoad = new JSONObject();
        List<JSONObject> other = new LinkedList<>();//其他设备
        //封装第一层
        for (UpsInfo u : upss) {
            if ("A总".equals(u.getGroupName())) {
                aRoad.put("deviceName", u.getDeviceName());
                aRoad.put("power", u.getPower());
                aRoad.put("loadRate", u.getLoadRate());
                aRoad.put("groupName", u.getGroupName());
                aRoad.put("alarm", u.getAlarm());
            }
            if ("B总".equals(u.getGroupName())) {
                bRoad.put("deviceName", u.getDeviceName());
                bRoad.put("power", u.getPower());
                bRoad.put("loadRate", u.getLoadRate());
                bRoad.put("groupName", u.getGroupName());
                bRoad.put("alarm", u.getAlarm());
            }
            if ("其他".equals(u.getGroupName())) {
                JSONObject j = new JSONObject();
                j.put("deviceName", u.getDeviceName());
                j.put("power", u.getPower());
                j.put("loadRate", u.getLoadRate());
                j.put("backTime", u.getBackTime());
                j.put("groupName", u.getGroupName());
                j.put("alarm", u.getAlarm());
                other.add(j);
            }
        }
        list.add(aRoad);
        list.add(bRoad);
        //封装虚拟A路、B路设备
        for (JSONObject json : list) {
            if (json.size() != 0) {
                String prefix = json.getString("groupName").substring(0, 1);
                List<JSONObject> l = new LinkedList<>();//下级UPS设备
                Double backTime = 0D;//电池剩余时间
                for (UpsInfo u : upss) {
                    String groupName = u.getGroupName();
                    if (StringUtils.isNotBlank(groupName)) {
                        if (groupName.equals(prefix + "分")) {
                            JSONObject j = new JSONObject();
                            j.put("deviceName", u.getDeviceName());
                            j.put("power", u.getPower());
                            j.put("loadRate", u.getLoadRate());
                            j.put("groupName", u.getGroupName());
                            j.put("alarm", u.getAlarm());
                            l.add(j);
                            //下级电池剩余时间加和
                            if (NumberUtils.isNumeric(u.getBackTime())) backTime += Double.parseDouble(u.getBackTime());
                        }
                    }
                }
                json.put("list", l);
                json.put("backTime", backTime + "");
            }
        }
        res.put("list", list);
        res.put("other", other);
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<List<AirInfo>> getAirs(String bankId) {
        //参数带bankId是移动端上级银行跳转下级银行
        if (StringUtils.isBlank(bankId)) {
            //不带bankId是移动端首页访问
            bankId = UserContext.getUserBankId();
        }
        List<Device> devicesAll = this.getAccountDevice(bankId, DeviceTypeEnum.AIR.getType());
        //只展示当前行设备
        List<Device> devices = new ArrayList<>();
        for (Device device : devicesAll) {
            if (device.getBankId().equals(bankId)) devices.add(device);
        }
        if (devices.size() == 0) return BaseResult.success(null, String.format("银行[%s]没有配置设备项！", bankId));
        //获取空调设备
        Map<String, AirInfo> airMap = new HashMap<>();
        List<AirInfo> airs = new ArrayList<>();
        List<String> devIds = new ArrayList<>();
        for (Device dev : devices) {
            AirInfo air = new AirInfo(dev);
            airs.add(air);
            devIds.add(dev.getDeviceId());
            airMap.put(dev.getDeviceId(), air);
        }
        //获取测点
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        if (spots.size() == 0) return BaseResult.success(null, String.format("银行[%s]没有配置测点！", bankId));
        Map<String, Spot> spotMap = new HashMap<>();
        List<String> spotIds = new ArrayList<>();
        for (Spot spot : spots) {
            spotIds.add(spot.getSpotId());
            spotMap.put(spot.getSpotId(), spot);
        }
        List<Monitor> ms = this.getMonitorList(spotIds);
        if (ms.size() == 0) return BaseResult.success(null, "没有查到测点数据！");
        //封装回风温度、回风湿度
        for (Monitor mo : ms) {
            Spot spot = spotMap.get(mo.getResource_id());
            AirInfo air = airMap.get(spot.getDeviceId());
            air.setState(mo.getStatus());
            switch (spot.getSpotType()) {
                case DevSpotConstant.SPOT_BACK_TEMPER:
                    air.setBackTemper(mo.getReal_value());
                    break;
                case DevSpotConstant.SPOT_BACK_HUMIDITY:
                    air.setBackHumidity(mo.getReal_value());
                    break;
                default:
            }
        }
        //查询空调设备状态
        List<ResourceItem> resources = new ArrayList<>();
        for (AirInfo air : airs) {
            resources.add(new ResourceItem(air.getDeviceId()));
        }
        MonitorRpcParam param = new MonitorRpcParam(resources);
        String resultStr = monitorRpc.getMonitorList(InfrasConstant.KE_RPC_COOKIE, param);
        JSONObject obj = JSONObject.parseObject(resultStr);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) return BaseResult.fail("查询Ke工程组态-设备通信状态失败！");
        String data = obj.getString("data");
        MonitorResult monitorResult = JSON.parseObject(data, MonitorResult.class);
        List<Monitor> resource = monitorResult.getResources();
        HashMap<String, Integer> map = new HashMap<>();
        resource.forEach((a) -> map.put(a.getResource_id(), a.getStatus()));
        for (AirInfo air : airs) {
            air.setState(map.get(air.getDeviceId()));
        }
        return BaseResult.success(airs, airs.size());
    }

    @Override
    public BaseResult<List<Humidity>> getHumids(String bankId) {
        List<Humidity> humids = new ArrayList<>();
        List<String> devIds = new ArrayList<>();
        List<Device> devices;
        //参数带bankId是PC端查询温湿度
        if (StringUtils.isNotBlank(bankId)) {
            devices = getBankDevice(bankId, DeviceTypeEnum.HUM.getType());
        } else {
            //参数不带bankId是移动端查询
            bankId = UserContext.getUserBankId();
            devices = getAccountDevice(bankId, DeviceTypeEnum.HUM.getType());
        }
        if (devices.size() == 0) return BaseResult.success(humids);
        //查到设备逻辑
        Map<String, Humidity> humMap = new HashMap<>();
        for (Device dev : devices) {
            Humidity air = new Humidity(dev);
            humids.add(air);
            devIds.add(dev.getDeviceId());
            humMap.put(dev.getDeviceId(), air);
        }
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        if (spots.size() == 0) return BaseResult.success(humids);
        //查到测点逻辑
        Map<String, Spot> spotMap = new HashMap<>();
        List<String> spotIds = new ArrayList<>();
        for (Spot spot : spots) {
            spotIds.add(spot.getSpotId());
            spotMap.put(spot.getSpotId(), spot);
        }
        List<Monitor> ms = this.getMonitorList(spotIds);
        if (ms.size() == 0) return BaseResult.fail("没有查到测点数据！");
        for (Monitor mo : ms) {
            Spot spot = spotMap.get(mo.getResource_id());
            Humidity hum = humMap.get(spot.getDeviceId());
            hum.setState(hum.getState() != 0 && hum.getState() != 1 ? 5 : mo.getStatus());
            switch (spot.getSpotType()) {
                case DevSpotConstant.SPOT_HUM_TEMP:
                    hum.setTemper(mo.getReal_value());
                    break;
                case DevSpotConstant.SPOT_HUM_HUMI:
                    hum.setHumidity(mo.getReal_value());
                    break;
                default:
            }
        }
//        //告警状态设置
//        Label label = new Label("allLevel", "1,2,3,4,5");
//        EventParam eventParam = EventParamUtils.createEventParam(label);
//        BaseResult<List<Event>> rEvents = eventService.getEventLast(eventParam);
//        if (!rEvents.isSuccess()) return BaseResult.fail(rEvents.getMessage());
//        List<Event> events = rEvents.getData();
//        for (Event event : events) {
//            if (spotIds.contains(event.getResource_id())) {
//                Spot spot = spotMap.get(event.getResource_id());
//                Humidity hum = humMap.get(spot.getDeviceId());
//                hum.setState(1);
//            }
//        }
        return BaseResult.success(humids, humids.size());
    }

    @Override
    public List<Monitor> getMonitorList(String leafId) {
        List<String> ids = Arrays.asList(leafId);
        return this.getMonitorList(ids);
    }

    @Override
    public List<Monitor> getMonitorList(List<String> leafIds) {
        List<ResourceItem> resources = new ArrayList<>();
        for (String leafId : leafIds) {
            resources.add(new ResourceItem(leafId));
        }
        MonitorRpcParam param = new MonitorRpcParam(resources);
        String resultStr = monitorRpc.getMonitorList(InfrasConstant.KE_RPC_COOKIE, param);
        JSONObject obj = JSONObject.parseObject(resultStr);
        String code = obj.getString("error_code");
        if ("00".equals(code)) {
            String data = obj.getString("data");
            MonitorResult monitorResult = JSON.parseObject(data, MonitorResult.class);
            return monitorResult.getResources();
        }
        throw new DataErrorException("查询测点数据失败！");
    }

    /**
     * 取得下一级银行s设备s
     */
    @Override
    public List<Device> getAccountDevice(String bankId, Integer type) {
        Bank bank = bankService.getBankById(bankId);
        List<Bank> subBanks = new ArrayList<>();
        if (!InfrasConstant.HEAD_BANK_ID.equals(bank.getBankId())) {
            subBanks.add(bank);
        }
        List<Bank> bks = bankMapper.getSubBanksById(bankId);
        subBanks.addAll(bks);
        List<String> bankIds = new ArrayList<>();
        Map<String, String> idNameMap = new HashMap<>();
        for (Bank bk : subBanks) {
            idNameMap.put(bk.getBankId(), bk.getBankName());
            bankIds.add(bk.getBankId());
        }
        Map<String, Object> param = new HashMap<>();
        param.put("bankIds", bankIds);
        param.put("deviceType", type);
        List<Device> devices = deviceSpotMapper.getBanksDevices(param);
        for (Device dev : devices) {
            dev.setBankName(idNameMap.get(dev.getBankId()));
        }
        return devices;
    }

    @Override
    public List<Device> getBankDevice(String bankId, int type) {
        Bank bank = bankService.getBankById(bankId);
        List<String> bankIds = Arrays.asList(bankId);
        Map<String, Object> param = new HashMap<>();
        param.put("bankIds", bankIds);
        param.put("deviceType", type);
        List<Device> devices = deviceSpotMapper.getBanksDevices(param);
        for (Device dev : devices) {
            dev.setBankName(bank.getBankName());
        }
        return devices;
    }

    @Override
    public List<JSONObject> childBankData(String bankId) {
        List<JSONObject> res = new LinkedList<>();
        //封装当前分行
        JSONObject j = new JSONObject();
        Bank bank1 = bankService.getBankById(bankId);
        j.put("name", bank1.getBankName());
        j.put("bankId", bankId);
        res.add(j);
        //封装下属银行
        List<Bank> banks = bankMapper.selectSubsById(bankId);
        for (Bank bank : banks) {
            JSONObject b = new JSONObject();
            String id = bank.getBankId();
            b.put("name", bank.getBankName());
            b.put("bankId", id);
            res.add(b);
        }
        return res;
    }
}
