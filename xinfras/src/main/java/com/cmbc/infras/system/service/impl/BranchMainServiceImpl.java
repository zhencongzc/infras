package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.DevSpotConstant;
import com.cmbc.infras.constant.DeviceTypeEnum;
import com.cmbc.infras.constant.DicConstant;
import com.cmbc.infras.constant.FLowFormStatusEnum;
import com.cmbc.infras.dto.*;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.CountDoneResult;
import com.cmbc.infras.dto.event.SubBankGroup;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.dto.ops.*;
import com.cmbc.infras.dto.rpc.Monitor;
import com.cmbc.infras.dto.rpc.alarm.WhereCountIterm;
import com.cmbc.infras.system.mapper.BankMapper;
import com.cmbc.infras.system.mapper.DeviceSpotMapper;
import com.cmbc.infras.system.mapper.MonitorMapper;
import com.cmbc.infras.system.rpc.FlowFormRpc;
import com.cmbc.infras.system.rpc.HealthRpc;
import com.cmbc.infras.system.rpc.HistoryAlarmRpc;
import com.cmbc.infras.system.service.*;
import com.cmbc.infras.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@Service
public class BranchMainServiceImpl implements BranchMainService {

    @Resource
    private BankService bankService;
    @Resource
    private MonitorService monitorService;
    @Resource
    private HeadMainService headMainService;
    @Resource
    private HistoryAlarmService historyAlarmService;

    @Resource
    private BankMapper bankMapper;
    @Resource
    private MonitorMapper monitorMapper;
    @Resource
    private DeviceSpotMapper deviceSpotMapper;

    @Resource
    private HealthRpc healthRpc;
    @Resource
    private FlowFormRpc flowFormRpc;
    @Resource
    private HistoryAlarmRpc historyAlarmRpc;

    /**
     * param:bankId
     */
    @Override
    public BaseResult<Energy> getEnergyData(BaseParam param) {
        //参数中带bankId,说明是总行跳转
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        Energy energy = new Energy();
        //查银行下的PUE设备,PUE设备只有一个
        Assert.hasLength(bankId, String.format("账号[%s]bankId为空！", bankId));
        Map<String, Object> map = new HashMap<>();
        map.put("bankId", bankId);
        map.put("deviceType", DeviceTypeEnum.PUE.getType());
        List<Device> devs = deviceSpotMapper.getDevByIdType(map);
        Assert.notEmpty(devs, String.format("银行[%s]没有配置PUE设置", bankId));
        Device pue = devs.get(0);

        Map<String, Integer> spotTypeMap = new HashMap<>();
        List<String> sids = new ArrayList<>();
        List<Spot> spots = monitorMapper.getDevSpots(pue.getDeviceId());
        for (Spot spot : spots) {
            spotTypeMap.put(spot.getSpotId(), spot.getSpotType());
            sids.add(spot.getSpotId());
        }

        List<Monitor> monitors = monitorService.getMonitorList(sids);
        for (Monitor mo : monitors) {
            Integer type = spotTypeMap.get(mo.getResource_id());
            switch (type) {
                case DevSpotConstant.PUE_REAL_TIME:
                    energy.setPue(mo.getReal_value());
                    break;
                case DevSpotConstant.PUE_TOTAL_POWER:
                    energy.setPower(mo.getReal_value());
                    break;
                case DevSpotConstant.PUE_IT_POWER:
                    energy.setItPower(mo.getReal_value());
                    break;
                default:
            }
        }
        energy.setTime(DateTimeUtils.getCurrentTime("HH:mm:ss"));
        return BaseResult.success(energy);
    }

    @Override
    public BaseResult<List<Humiture>> getHumiture(BaseParam param) {
        //参数中带bankId,说明是总行跳转，不带则取UserContext账号绑定银行
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        Assert.hasLength(bankId, String.format("机房温湿度,账号[%s]没有绑定银行", UserContext.getUserAccount()));
        List<Humiture> hums = new ArrayList<>();
        SpotIds spotIds = new SpotIds();
        Bank bank = bankService.getBankById(bankId);
        getSubBanks(bank, spotIds, DeviceTypeEnum.HUM.getType());
        Map<String, Monitor> moMap = new HashMap<>();
        List<Monitor> monitors = monitorService.getMonitorList(spotIds.getResourceIds());
        for (Monitor mo : monitors) {
            moMap.put(mo.getResource_id(), mo);
        }
        //当前银行数据
        if (!"0".equals(bankId)) {
            Humiture nowHum = getBankHumiture(bank, moMap);
            hums.add(nowHum);
        }
        //二级分行-数据
        List<Bank> fenhang2 = bank.getSubs();
        List<Bank> zhihang = new ArrayList<>();
        for (Bank bk : fenhang2) {
            zhihang.addAll(bk.getSubs());
            Humiture hum = getBankHumiture(bk, moMap);
            hums.add(hum);
        }
        //支行-数据
        List<Bank> cunzhen = new ArrayList<>();
        for (Bank bk2 : zhihang) {
            cunzhen.addAll(bk2.getSubs());
            Humiture hum = getBankHumiture(bk2, moMap);
            hums.add(hum);
        }
        //村镇银行数据-数据
        for (Bank bk3 : cunzhen) {
            Humiture hum = getBankHumiture(bk3, moMap);
            hums.add(hum);
        }
        return BaseResult.success(hums, hums.size());
    }

    /**
     * 递归查询下级银行
     * 设置bank.subBanks,bank.devices(type温湿度)
     * spotId--resourceId放到spotIds.resourceIds中
     */
    private void getSubBanks(Bank bank, SpotIds spotIds, Integer deviceType) {
        //查下级银行,设置subs
        List<Bank> subs = bankMapper.getSubBanksById(bank.getBankId());
        bank.setSubs(subs);
        //查银行下设备-温湿度类型
        Map<String, Object> param = new HashMap<>();
        param.put("bankId", bank.getBankId());
        param.put("deviceType", deviceType);
        List<Device> devs = deviceSpotMapper.getDevices(param);
        //Assert.notEmpty(devs, String.format("银行[%s]没有配置设备", bank.getBankId()));
        bank.setDevices(devs);
        for (Device dev : devs) {
            //查设备下测点
            List<Spot> spots = deviceSpotMapper.getDevSpots(dev);
            //Assert.notEmpty(spots, String.format("银行[%s]-设备[%s]没有配置测点", bank.getBankName(), dev.getDeviceName()));
            dev.setSpots(spots);
            spotIds.addAll(spots);
        }
        /**
         * level==4是村镇
         * 村镇银行没有下一级
         */
        if (bank.getLevel() == 4) {
            return;
        }
        for (Bank sub : subs) {
            getSubBanks(sub, spotIds, deviceType);
        }
    }

    /**
     * 取出银行下设备-(温湿度)
     * 取出设备下测点
     * 匹配测点值-moMap
     * 得到银行下测点温度集合/湿度集合
     * 排序取最大最小值
     * 返回温湿度
     */
    private Humiture getBankHumiture(Bank bank, Map<String, Monitor> moMap) {
        Humiture hum = new Humiture();
        hum.setBankId(bank.getBankId());
        hum.setBankName(bank.getBankName());
        List<Float> tempers = new ArrayList<>();
        List<Float> humidis = new ArrayList<>();
        List<Device> ds = bank.getDevices();
        if (ds.isEmpty()) return hum.initEmpty();
        for (Device d : ds) {
            List<Spot> ss = d.getSpots();
            if (ss.isEmpty()) return hum;
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
                            System.out.println("getBankHumiture-switch-default:" + v);
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
        return hum;
    }

    @Override
    public BaseResult<DisposeRate> getDisposeRate(BaseParam param) {
        //参数中带bankId,说明是总行跳转
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        Assert.hasLength(bankId, String.format("告警处理率,账号[%s]没有绑定银行", UserContext.getUserAccount()));
        List<String> bankIds = bankService.getSubBankIds(Arrays.asList(bankId));
        //统计不是总行0的银行
        if (!"0".equals(bankId)) bankIds.add(bankId);
        HashMap<String, Integer> alarmCount = historyAlarmService.getHistoryAlarmCount(bankIds, "is_accept");
        DisposeRate disposeRate = new DisposeRate(alarmCount);
        return BaseResult.success(disposeRate);
    }

    @Override
    public BaseResult<GradeRate> getGradeRate(BaseParam param) {
        //参数中带bankId,说明是总行跳转
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) {
            bankId = UserContext.getUserBankId();
        }
        Assert.hasLength(bankId, String.format("告警级别分类,账号[%s]没有绑定银行", UserContext.getUserAccount()));
        List<String> bankIds = bankService.getSubBankIds(Arrays.asList(bankId));
        //如果不是总行0，则添加上此银行
        if (!"0".equals(bankId)) bankIds.add(bankId);
        //查询告警数据
        String eventLevel = "1,2,3,4,5";
        String account = UserContext.getUserAccount();
        WhereCountIterm whereParam = AlarmParamUtils.createHistoryCountParam(bankIds, eventLevel, null);
        String cookie = AlarmParamUtils.createCookie(account);
        String str = historyAlarmRpc.getHistoryAlarmCount(cookie, whereParam);
        AlarmCount alarmCount = AlarmParamUtils.parseCountResult(str);
        GradeRate gradeRate;
        if (alarmCount == null) {
            gradeRate = new GradeRate(0);
        } else {
            gradeRate = new GradeRate(alarmCount);
        }
        return BaseResult.success(gradeRate);
    }

    @Override
    public BaseResult<Capacity> getCapacity(BaseParam param) {
        //参数中带bankId,说明是总行跳转
        String bankId = param.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        Assert.hasLength(bankId, String.format("账号[%s]没有绑定银行", UserContext.getUserAccount()));
        Capacity cap = new Capacity();
        SpotIds spotIds = new SpotIds();
        Map<String, Spot> spotIdMap = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        map.put("bankId", bankId);
        map.put("deviceType", DeviceTypeEnum.AREA.getType());
        //空间设备
        List<Device> areas = deviceSpotMapper.getDevices(map);
        if (areas.isEmpty()) return BaseResult.success(cap.kong());
        Device area = areas.get(0);
        List<Spot> areaSpots = deviceSpotMapper.getDevSpots(area);
        area.setSpots(areaSpots);
        spotIds.addAll(areaSpots);
        fillMap(spotIdMap, areaSpots);
        //配电设备
        map.put("deviceType", DeviceTypeEnum.ELEC.getType());
        List<Device> elecs = deviceSpotMapper.getDevices(map);
        if (elecs.isEmpty()) return BaseResult.success(cap.kong());
        Device elec = elecs.get(0);
        List<Spot> elecSpots = deviceSpotMapper.getDevSpots(elec);
        elec.setSpots(elecSpots);
        spotIds.addAll(elecSpots);
        fillMap(spotIdMap, elecSpots);
        //制冷设备
        map.put("deviceType", DeviceTypeEnum.COOL.getType());
        List<Device> cools = deviceSpotMapper.getDevices(map);
        if (cools.isEmpty()) return BaseResult.success(cap.kong());
        Device cool = cools.get(0);
        List<Spot> coolSpots = deviceSpotMapper.getDevSpots(cool);
        cool.setSpots(coolSpots);
        spotIds.addAll(coolSpots);
        fillMap(spotIdMap, coolSpots);
        List<Monitor> monitors = monitorService.getMonitorList(spotIds.getResourceIds());
        DecimalFormat df = new DecimalFormat("#0.0");
        for (Monitor mo : monitors) {
            Spot s = spotIdMap.get(mo.getResource_id());
            String realValue = mo.getReal_value();
            String value = NumberUtils.isNumeric(realValue) ? df.format(Double.valueOf(realValue)) : realValue;
            switch (s.getSpotType()) {
                case DicConstant.ENERGY_AREA_SUM:
                    cap.setSpaceTotal(value);
                    break;
                case DicConstant.ENERGY_AREA_USE:
                    cap.setSpaceUsed(value);
                    break;
                case DicConstant.ENERGY_AREA_PER:
                    cap.setSpaceRate(value);
                    break;
                case DicConstant.ENERGY_ELEC_SUM:
                    cap.setPowerTotal(value);
                    break;
                case DicConstant.ENERGY_ELEC_USE:
                    cap.setPowerUsed(value);
                    break;
                case DicConstant.ENERGY_ELEC_PER:
                    cap.setPowerRate(value);
                    break;
                case DicConstant.ENERGY_COOL_SUM:
                    cap.setCoolTotal(value);
                    break;
                case DicConstant.ENERGY_COOL_USE:
                    cap.setCoolUsed(value);
                    break;
                case DicConstant.ENERGY_COOL_PER:
                    cap.setCoolRate(value);
                    break;
                default:
            }
        }
        return BaseResult.success(cap);
    }

    private void fillMap(Map<String, Spot> spotIdMap, List<Spot> spots) {
        for (Spot spot : spots) {
            spotIdMap.put(spot.getSpotId(), spot);
        }
    }

    @Override
    public BaseResult<List<BranchRadar>> getBranchRadar(BaseParam param) {
        String bankId = param.getBankId();
        //为空则是分行账号
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        Assert.hasLength(bankId, String.format("告警雷达,账号[%s]没有绑定银行", UserContext.getUserAccount()));
        List<BranchRadar> radars = new ArrayList<>();
        Bank bank = bankService.getBankById(bankId);
        SubBankGroup group = bankService.getSubBanksGroup(bankId);
        List<Bank> banks = new ArrayList<>();
        if (!bank.getBankId().equals("0")) banks.add(bank);
        banks.addAll(group.getBranchBanks());
        banks.addAll(group.getSubBanks());
        banks.addAll(group.getTownBanks());
        for (Bank bk : banks) {
            CountDoneResult result = historyAlarmService.countDoneWithLevel(Arrays.asList(bk.getBankId()), "1,2,3");
            BranchRadar radar = new BranchRadar(bk, result);
            radars.add(radar);
        }
        return BaseResult.success(radars, radars.size());
    }

    @Override
    public BaseResult<Maintain> getMaintainRate(BaseParam param) {
        String bankId = param.getBankId();
        //参数无bankId,查当前用户下数据
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        Bank bank = bankService.getBankById(bankId);
        Assert.notNull(bank, String.format("没有ID为[%s]的银行信息", bankId));
        String bankName = bank.getBankName();
        Assert.hasLength(bankName, String.format("Bank[ID:%s]信息异常,bankName:%s", bankId, bankName));
        //作业复核
        List<JSONObject> data = headMainService.getWorkCheck().getData();
        int total = 0;
        int closed = 0;
        for (JSONObject d : data) {
            if (bankName.equals(d.getString("bankName"))) {
                total++;
                if ("已关闭".equals(d.getString("stateName"))) closed++;
            }
        }
        String workCheck = NumberUtils.getPersent(closed, total);
        //巡检复核
        List<JSONObject> data1 = headMainService.getPartol().getData();
        int total1 = 0;
        int closed1 = 0;
        for (JSONObject d : data1) {
            if (bankName.equals(d.getString("bankName"))) {
                total1++;
                if ("已关闭".equals(d.getString("stateName"))) closed1++;
            }
        }
        String patrolCheck = NumberUtils.getPersent(closed1, total1);
        //实时得分,年平均分
        String scopt;
        String average;
        JSONObject scoreParam = new JSONObject();
        scoreParam.put("account", "admin");
        scoreParam.put("modelId", YmlConfig.scoreFormId);
        scoreParam.put("name", bankName);
        String str = healthRpc.getScoreData(scoreParam);
        JSONObject obj = JSONObject.parseObject(str);
        if (obj.getIntValue("state") != 200)
            throw new RuntimeException(String.format("查询实时得分,年  平均分异常!%s,%s", obj.getString("message"), bankName));
        JSONObject data2 = obj.getJSONObject("data");
        scopt = data2.getString("result");
        average = data2.getString("average");
        //结果
        Maintain maintain = new Maintain(workCheck, patrolCheck, scopt, average);
        return BaseResult.success(maintain);
    }


}
