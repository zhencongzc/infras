package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.DevSpotConstant;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.constant.SpotTypeEnum;
import com.cmbc.infras.dto.*;
import com.cmbc.infras.dto.monitor.Humidity;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.dto.monitor.UpsInfo;
import com.cmbc.infras.dto.ops.Asset;
import com.cmbc.infras.dto.ops.OpsBankInfo;
import com.cmbc.infras.dto.rpc.Monitor;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.dto.rpc.event.EventParam;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.exception.DataErrorException;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DataConfigMapper;
import com.cmbc.infras.system.mapper.DeviceSpotMapper;
import com.cmbc.infras.system.mapper.MonitorMapper;
import com.cmbc.infras.system.service.*;
import com.cmbc.infras.system.util.BusinessUtil;
import com.cmbc.infras.util.EventParamUtils;
import com.cmbc.infras.util.NumberUtils;
import com.cmbc.infras.util.UserContext;
import com.cmbc.infras.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@Service
public class BranchOpsServiceImpl implements BranchOpsService {

    Logger LOG = LoggerFactory.getLogger("ExecuteAspect");

    @Resource
    private MonitorService monitorService;
    @Resource
    private EventService eventService;
    @Resource
    private AriService ariService;
    @Resource
    private BankService bankService;
    @Resource
    private LabelService labelService;
    @Resource
    private FlowFormService flowFormService;
    @Resource
    private AlarmService alarmService;

    @Resource
    private DeviceSpotMapper deviceSpotMapper;
    @Resource
    private MonitorMapper monitorMapper;
    @Resource
    private DataConfigMapper dataConfigMapper;
    @Resource
    private BankMapper bankMapper;


    /**
     * 查询所有下级银行
     * param:bankId
     */
    @Override
    public BaseResult<List<Bank>> getAllBanks(BaseParam param) {
        //参数中带bankId,说明是总行跳转
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) {
            bankId = UserContext.getUserBankId();
            Assert.hasLength(bankId, String.format("账号[%s]没有绑定银行", UserContext.getUserAccount()));
        }
        List<Bank> banks = bankService.getSubBanks(Arrays.asList(bankId));
        /**
         * "main"是分行主界面
         * 需要查询本银行
         * 需要PUE值
         */
        if ("main".equals(param.getPage())) {
            //查询本银行
            Bank bank = bankService.getBankById(bankId);
            banks.add(0, bank);
            //计算PUE值
            for (Bank bk : banks) {
                Device pue = deviceSpotMapper.getBankPue(bk.getBankId());
                if (pue == null) {
                    bk.setPue("-*-");
                } else {
                    List<String> devIds = Arrays.asList(pue.getDeviceId());
                    List<Spot> spots = deviceSpotMapper.getDevicesSpots(devIds);
                    if (spots.isEmpty()) {
                        throw new DataErrorException(String.format("银行[%s]PUE设备[%s]没有配置测点！", bk.getBankId(), pue.getDeviceId()));
                    }
                    for (Spot spot : spots) {
                        if (spot.getSpotType() == SpotTypeEnum.PUE_REAL_TIME.getType()) {
                            List<Monitor> ms = monitorService.getMonitorList(spot.getSpotId());
                            bk.setPue(ms.get(0).getReal_value());
                        }
                    }
                }
            }
        }
        return BaseResult.success(banks, banks.size());
    }

    @Override
    public BaseResult<OpsBankInfo> getBankInfos(BaseParam param) throws Exception {
        //参数中带bankId,说明是总行跳转
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        //UPS
        OpsBankInfo opsBank = new OpsBankInfo();
        BaseResult<List<UpsInfo>> rUpss = monitorService.getUpss(bankId);
        if (!rUpss.isSuccess()) return BaseResult.fail(rUpss.getMessage());
        opsBank.setUpss(rUpss.getData());
        //温湿度
        BaseResult<List<Humidity>> rHumidity = monitorService.getHumids(bankId);
        if (!rHumidity.isSuccess()) return BaseResult.fail(rHumidity.getMessage());
        Humidity hum = new Humidity();
        List<Humidity> listHum = rHumidity.getData();
        List<Float> listTemper = new LinkedList<>();
        List<Float> listHumidity = new LinkedList<>();
        for (Humidity h : listHum) {
            String temper = h.getTemper();
            String humidity = h.getHumidity();
            if (!temper.equals("-")) listTemper.add(Float.parseFloat(temper));
            if (!humidity.equals("-")) listHumidity.add(Float.parseFloat(humidity));
        }
        float sumTemper = 0f;
        for (Float t : listTemper) {
            sumTemper += t;
        }
        float sumHumidity = 0f;
        for (Float h : listHumidity) {
            sumHumidity += h;
        }
        hum.setTemper(String.valueOf(NumberUtils.fomatFloat((sumTemper / listTemper.size()), 2)));
        DecimalFormat df = new DecimalFormat("#0.0");
        hum.setHumidity(df.format(NumberUtils.fomatFloat((sumHumidity / listHumidity.size()), 2)));
        opsBank.setHumidity(hum);
        //空调告警
        //查询出账号下银行下的所有空调测点
        BaseResult<List<String>> rIds = ariService.getUserAirSpot(bankId);
        if (!rIds.isSuccess()) return BaseResult.fail(rIds.getMessage());
        List<String> resourceIds = rIds.getData();
        //查看这些测点下是否有告警信息,如果有,则设置告警为1(前台显示红字告警字样)
        BaseResult<List<Event>> rEvents = eventService.getEventLast(null);
        if (!rEvents.isSuccess()) return BaseResult.fail(rEvents.getMessage());
        List<Event> events = rEvents.getData();
        for (Event event : events) {
            if (resourceIds.contains(event.getResource_id())) {
                opsBank.setAirAlarm(1);
                break;
            }
        }
        return BaseResult.success(opsBank);
    }

    @Override
    public BaseResult<List<AlarmEvent>> getAlarmInfos(BaseParam baseParam) {
        //获取告警信息
        String sessionId = UserContext.getAuthToken();
        List<AlarmEvent> datas = new ArrayList<>();
        String account = UserContext.getUserAccount();
        Label label = labelService.getUserLabel(account);
        EventParam param = EventParamUtils.createEventParam(label);
        BaseResult<List<AlarmEvent>> allAlarm = alarmService.getAllAlarm(param);
        if (!allAlarm.isSuccess()) return allAlarm;
        List<AlarmEvent> events = allAlarm.getData();
        //过滤告警，只显示当天的告警
        long todayZeroDot = DateTimeUtils.getTodayZeroDot();
        Iterator<AlarmEvent> iterator = events.iterator();
        while ((iterator.hasNext())) {
            AlarmEvent event = iterator.next();
            if (event.getEventTime() < todayZeroDot) {
                iterator.remove();
            } else {
                //修改content内容
                event.setContent(event.getContent() + "，当前值为：" + event.getEventSnapshot());
            }
        }
        //如有bankId进行过滤
        String bankId = baseParam.getBankId();
        if (StringUtils.isNotBlank(bankId)) {
            List<String> bankIds = flowFormService.getCacheSubBankIds(bankId, sessionId);
            bankIds.add(bankId);//加上当前行的
            for (AlarmEvent event : events) {
                if (BusinessUtil.alarmMatchBankId(event, bankIds)) datas.add(event);
            }
        } else {
            datas = events;
        }
        //增加联系人信息
        if (!YmlConfig.getBoolValue("noContact")) {
            HashSet<String> ids = flowFormService.getAllBankIds(sessionId);
            for (AlarmEvent event : datas) {
                String location = event.getEventLocation(); //project_root/0_931/0_969/0_1083/0_1183
                String[] arr = location.split("/");
                String rid = arr.length > 2 ? arr[2] : "-1";
                if (ids.contains(rid)) {
                    Bank bank = flowFormService.getBankById(rid, sessionId);
                    if (bank == null) {
                        LOG.error("BranchOpsServiceImpl-增加联系人信息 getBankById bank is null delete redis key:infras_all_bank_ids");
                        DataRedisUtil.delete(InfrasConstant.INFRAS_ALL_BANK_IDS);
                        event.setContact("--");
                    } else {
                        String contact = bank.getContact();
                        if (StringUtils.isBlank(contact)) {
                            event.setContact("-");
                        } else {
                            event.setContact(contact);
                        }
                    }
                }
            }
        }
        return BaseResult.success(datas, datas.size());
    }

    @Override
    public BaseResult<List<Bank>> getLowerBank(BaseParam param) {
        List<Bank> banks = null;
        try {
            String bankId = UserContext.getUserBankId();
            Assert.notNull(bankId, "没有取到银行信息！");
            //总行用户-只查下一级,不查总行0
            banks = bankMapper.getSubBanksById(bankId);
            LOG.info("BranchOpsServiceImpl.getLowerBank->getSubBanksById bankId:{}, banks:{}", bankId, JSON.toJSONString(banks));
            //取得PUE值-getAllBanks同
            for (Bank bk : banks) {
                Device pue = deviceSpotMapper.getBankPue(bk.getBankId());
                LOG.info("BranchOpsServiceImpl.getLowerBank getBankPud bankId:{}, pue:{}", bk.getBankId(), JSON.toJSONString(pue));
                if (pue == null) {
                    bk.setPue("-");
                } else {
                    List<String> devIds = Arrays.asList(pue.getDeviceId());
                    List<Spot> spots = deviceSpotMapper.getDevicesSpots(devIds);
                    LOG.info("BranchOpsServiceImpl.getLowerBank getDevicesSpots bankId:{}, devId:{}, spots:{}", bk.getBankId(), pue.getDeviceId(), JSON.toJSONString(spots));
                    if (spots.isEmpty()) {
                        LOG.error("BranchOpsServiceImpl.getLowerBank bank[{}]'s devId[{}]'s spot is empty", bk.getBankId(), pue.getDeviceId());
                        throw new DataErrorException(String.format("银行[%s]PUE设备[%s]没有配置测点！", bk.getBankId(), pue.getDeviceId()));
                    }
                    for (Spot spot : spots) {
                        if (spot.getSpotType() == SpotTypeEnum.PUE_REAL_TIME.getType()) {
                            List<Monitor> ms = monitorService.getMonitorList(spot.getSpotId());
                            bk.setPue(ms.get(0).getReal_value());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("BranchOpsServiceImpl.getLowerBank exception:{}", e.getMessage(), e);
            e.printStackTrace();
        }
        return BaseResult.success(banks, banks.size());
    }

    @Override
    public BaseResult<JSONObject> getBankAsset(BaseParam param) throws Exception {
        JSONObject res = new JSONObject();
        DecimalFormat df = new DecimalFormat("#0");
        Asset asset = new Asset();
        if (StringUtils.isBlank(param.getBankId())) {
            String bankId = UserContext.getUserBankId();
            param.setBankId(bankId);
        }
        Assert.hasLength(param.getBankId(), String.format("参数[bankId]不能为空！"));
        //查询所有设备
        List<Device> devices = dataConfigMapper.findBankDevices(param.getBankId());
        if (devices.isEmpty()) return BaseResult.success(new Asset(true));
        List<String> devIds = new ArrayList<>();
        Map<String, Device> devIdMap = new HashMap<>();
        for (Device dev : devices) {
            devIdMap.put(dev.getDeviceId(), dev);
            devIds.add(dev.getDeviceId());
            switch (dev.getDeviceType()) {
                case DevSpotConstant.DEV_UPS:
                    //只展示A总、B总虚拟设备
                    if (dev.getGroupName() != null) {
                        if (dev.getGroupName().contains("总")) {
                            asset.getUpss().add(dev);
                        }
                    }
                    break;
                case DevSpotConstant.DEV_PUE:
                    asset.setPueDev(dev);
                    break;
                case DevSpotConstant.DEV_CHAI_FA:
                    asset.setChaifa(dev);
                    break;
                default:
            }
        }
        //查询相关测点
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        List<String> resourceIds = new ArrayList<>();
        for (Spot spot : spots) {
            devIdMap.get(spot.getDeviceId()).getSpots().add(spot);
            resourceIds.add(spot.getSpotId());
        }
        //查询测点数据
        Map<String, Monitor> idMonitorMap = new HashMap<>();
        List<Monitor> monitors = monitorService.getMonitorList(resourceIds);
        for (Monitor mo : monitors) {
            idMonitorMap.put(mo.getResource_id(), mo);
        }
        //获取分行运维ups数据
        List<JSONObject> list = new LinkedList<>();
        BaseResult<JSONObject> result = monitorService.upss(param.getBankId());
        List<JSONObject> listUps = result.getData().getJSONArray("list").toJavaList(JSONObject.class);
        //组装数据
        List<Device> upss = asset.getUpss();
        for (Device dev : upss) {
            JSONObject device = new JSONObject();
            device.put("name", dev.getDeviceName());
            List<Spot> sps = dev.getSpots();
            for (Spot sp : sps) {
                Monitor mo = idMonitorMap.get(sp.getSpotId());
                if (mo != null) {
                    switch (sp.getSpotType()) {
                        case DevSpotConstant.SPOT_UPS_LOAD:
                            JSONObject upsLoad = new JSONObject();
                            upsLoad.put("name", sp.getSpotName() + "(%)");
                            String val = "";
                            for (JSONObject l : listUps) {
                                if (l.size() != 0) {
                                    if (l.getString("deviceName").equals(dev.getDeviceName()))
                                        val = l.getString("loadRate");
                                }
                            }
                            upsLoad.put("val", val);
                            device.put("upsLoad", upsLoad);
                            break;
                        case DevSpotConstant.SPOT_UPS_TIME:
                            JSONObject backTime = new JSONObject();
                            backTime.put("name", sp.getSpotName() + "(min)");
                            String val1 = "";
                            for (JSONObject l : listUps) {
                                if (l.size() != 0) {
                                    if (l.getString("deviceName").equals(dev.getDeviceName()))
                                        val1 = l.getString("backTime");
                                }
                            }
                            backTime.put("val", val1);
                            device.put("backTime", backTime);
                            break;
                        default:
                    }
                }
            }
            list.add(device);
        }
        res.put("list", list);
        //计算pue
        JSONObject pue = new JSONObject();
        Device pue1 = asset.getPueDev();
        List<Spot> pueSpots;
        if (pue1 != null && !pue1.getSpots().isEmpty()) {
            pueSpots = pue1.getSpots();
            for (Spot spot : pueSpots) {
                Monitor mo = idMonitorMap.get(spot.getSpotId());
                if (mo != null) {
                    if (spot.getSpotType() == DevSpotConstant.PUE_REAL_TIME) {
                        pue.put("name", spot.getSpotName());
                        pue.put("val", mo.getReal_value());
                    }
                }
            }
        }
        res.put("pue", pue);
        //计算柴发后备时间
        JSONObject dynamoTime = new JSONObject();
        Device chai = asset.getChaifa();
        if (chai != null) {
            List<Spot> chais = chai.getSpots();
            if (!chais.isEmpty()) {
                for (Spot sp : chais) {
                    Monitor mo = idMonitorMap.get(sp.getSpotId());
                    if (mo != null) {
                        if (sp.getSpotType() == DevSpotConstant.CHAI_FA_TIME) {
                            dynamoTime.put("name", sp.getSpotName() + "(min)");
                            dynamoTime.put("val", NumberUtils.isNumeric(mo.getReal_value()) ? df.format(Double.valueOf(mo.getReal_value())) :
                                    mo.getReal_value());
                        }
                    }
                }
            }
        }
        res.put("dynamoTime", dynamoTime);
        return BaseResult.success(res);
    }

}
