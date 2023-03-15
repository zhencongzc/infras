package com.cmbc.infras.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.DeviceTypeEnum;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Device;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.EventInfo;
import com.cmbc.infras.dto.monitor.Spot;
import com.cmbc.infras.dto.rpc.event.*;
import com.cmbc.infras.system.mapper.MonitorMapper;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.service.EventService;
import com.cmbc.infras.system.service.MonitorService;
import com.cmbc.infras.system.util.InfoUtils;
import com.cmbc.infras.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
public class EventServiceImpl implements EventService {

    @Resource
    private EventRpc eventRpc;

    @Resource
    private MonitorMapper monitorMapper;

    @Resource
    private MonitorService monitorService;


    @Override
    public BaseResult<List<EventInfo>> events(String bankId) {
        List<EventInfo> infos = new ArrayList<>();
        List<Device> devices;
        //参数带bankId是PC端调用
        if (StringUtils.isNotBlank(bankId)) {
            devices = monitorService.getBankDevice(bankId, DeviceTypeEnum.UPS.getType());
        } else {
            //参数不带bankId是移动端页面调用
            bankId = UserContext.getUserBankId();
            devices = monitorService.getAccountDevice(bankId, DeviceTypeEnum.UPS.getType());
        }
        if (devices.size() == 0) {
            return BaseResult.fail(String.format("银行[%s]没有配置设备项！", bankId));
        }
        Map<String, Device> devIdMap = new HashMap<>();
        List<String> devIds = new ArrayList<>();
        for (Device dev : devices) {
            devIds.add(dev.getDeviceId());
            devIdMap.put(dev.getDeviceId(), dev);
        }
        List<Spot> spots = monitorMapper.getDevsSpots(devIds);
        if (spots.size() == 0) {
            return BaseResult.fail(String.format("银行[%s]没有配置测点！", bankId));
        }
        Map<String, Device> spotIdDevMap = new HashMap<>();
        List<String> spotIds = new ArrayList<>();
        for (Spot spot : spots) {
            spotIds.add(spot.getSpotId());
            spotIdDevMap.put(spot.getSpotId(), devIdMap.get(spot.getDeviceId()));
        }
        /**
         * 查询KE平台全量告警数据
         */
        BaseResult<List<Event>> rEvents = this.getEventLast(null);
        if (!rEvents.isSuccess()) {
            return BaseResult.fail(rEvents.getMessage());
        }
        List<Event> events = rEvents.getData();
        for (Event event : events) {
            String resourceId = event.getResource_id();
            if (spotIds.contains(resourceId)) {
                Device dev = spotIdDevMap.get(resourceId);
                String time = DateTimeUtils.transToStr(event.getEvent_time());
                EventInfo ei = new EventInfo(dev, event.getResource_id(),
                        event.getEvent_level(), time, event.getContent());
                infos.add(ei);
            }

        }
        return BaseResult.success(infos, infos.size());
    }

    @Override
    public Integer getAllAlarmCount(String account, String token) throws Exception {
        //token验证
        if (!checkToken(account, token)) throw new Exception("token验证失败!");
        EventGroupParam param = new EventGroupParam();
        param.setGroup("event_level");
        String countStr = "";
        try {
            countStr = eventRpc.getEventLastCount(AlarmParamUtils.createCookie(account), param);
        } catch (Exception e) {
            log.warn("用户获取KE实时告警出错！ url: {}, account: {}, result: {}", "/api/v3/tsdb/status/event/last/count", account, e.getMessage());
            throw e;
        }
        BaseResult<AlarmCount> res = parseCountResult(countStr);
        //只统计1，2，3，4，5的告警数量
        Map<String, Integer> group = res.getData().getGroup();
        int count = 0;
        if (group.get("1") != null) count += group.get("1");
        if (group.get("2") != null) count += group.get("2");
        if (group.get("3") != null) count += group.get("3");
        if (group.get("4") != null) count += group.get("4");
        if (group.get("5") != null) count += group.get("5");
        return count;
    }

    @Override
    public BaseResult<AlarmCount>   getEventLastCount(String bankId) {
        //组装入参
        EventGroupParam param = new EventGroupParam();
        List<QueryCondition> list = new ArrayList<>();
        list.add(new QueryCondition("is_confirm", "eq", 0));
        list.add(new QueryCondition("cep_processed", "eq", 0));
        WhereCondition where = new WhereCondition(list);
        List<SortCondition> sorts = new ArrayList() {{
            add(new SortCondition("event_time", "DESC"));
        }};
        PageCondition page = new PageCondition("1", InfrasConstant.ALARM_PAGE_SIZE);
        param.setWhere(where);
        param.setSorts(sorts);
        param.setPage(page);
        param.setGroup("event_level");
        param.setExtra(true);
        String account = UserContext.getUserAccount();
        //有bankId则筛选银行
        if (bankId != null && !"0".equals(bankId)) {
            JSONObject j = new JSONObject();
            List<QueryCondition> or = new LinkedList<>();
            or.add(new QueryCondition("event_location", "like_any", new String[]{"%/" + bankId + "/%"}));
            or.add(new QueryCondition("event_location", "like_any", new String[]{"%/" + bankId}));
            j.put("or", or);
            param.getWhere().getAnd().get(0).setOr(or);
        }
        String countStr = eventRpc.getEventLastCount(AlarmParamUtils.createCookie(account), param);
        return parseCountResult(countStr);
    }

    /**
     * 查询所有实时告警
     * 查询实时告警数量
     * 设置页大小为总数量一次查询
     */
    @Override
    public BaseResult<List<Event>> getEventLast(EventParam param) {
        String account = UserContext.getUserAccount();
        if (param == null) {
            param = new EventParam();
        }
        //KE实时告警最大数300(可设置),这里设成400不在查询数量
        param.getPage().setSize(InfrasConstant.ALARM_PAGE_SIZE);
        String resultStr = eventRpc.getEventLast(AlarmParamUtils.createCookie(account), param);
        List<Event> events = EventParamUtils.parseEventResult(resultStr);
        return BaseResult.success(events, events.size());
    }

    /**
     * 查询历史告警，分页查询合集（新）
     */
    @Override
    public BaseResult<List<Event>> getEvents(String account, JSONObject param) {
        String countStr = eventRpc.getEventCount(AlarmParamUtils.createCookie(account), param);
        BaseResult<AlarmCount> rCount = parseCountResult(countStr);
        if (!rCount.isSuccess()) return BaseResult.fail(rCount.getMessage());
        String str = eventRpc.getEvent(AlarmParamUtils.createCookie(account), param);
        List<Event> events = EventParamUtils.parseEventResult(str);
        BaseResult result = new BaseResult();
        result.setPageSize(param.getJSONObject("page").getIntValue("size"));
        result.setTotal(rCount.getData().getCount());
        result.setPageCount((param.getJSONObject("page").getIntValue("number")));
        result.setData(events);
        result.setSuccess(true);
        return result;
    }

    /**
     * 查询历史告警
     * 分页查询合集
     */
    @Override
    public BaseResult<List<Event>> getEvents(String account, EventParam param) {
        if (param == null) {
            param = new EventParam(false);
        }
        String countStr = eventRpc.getEventCount(AlarmParamUtils.createCookie(account), param);
        BaseResult<AlarmCount> rCount = parseCountResult(countStr);
        if (!rCount.isSuccess()) {
            return BaseResult.fail(rCount.getMessage());
        }

        String str = eventRpc.getEvent(AlarmParamUtils.createCookie(account), param);
        List<Event> events = EventParamUtils.parseEventResult(str);

        BaseResult result = new BaseResult();

        result.setPageSize(param.getPage().getSize());
        result.setTotal(rCount.getData().getCount());
        result.setPageCount(Integer.parseInt(param.getPage().getNumber()));
        result.setData(events);
        result.setSuccess(true);
        return result;

    }

    @Override
    public BaseResult<Boolean> alarmAccept(AlarmAcceptParam param) {
        try {
            String str = eventRpc.alarmAccept(InfrasConstant.KE_RPC_COOKIE, param);
            BaseResult<Boolean> result = parseAcceptResult(str);
            return result;
        } catch (Exception e) {
            String message = e.getMessage().substring(e.getMessage().lastIndexOf("{"), e.getMessage().lastIndexOf("}") + 1);
            JSONObject j = JSONObject.parseObject(message);
            if ("71".equals(j.getString("error_code"))) return BaseResult.fail("银行通信中断，受理失败");
            return BaseResult.fail(e.getMessage());
        }
    }

    @Override
    public BaseResult<Boolean> alarmConfirm(AlarmConfirmParam param) {
        try {
            String str = eventRpc.alarmConfirm(InfrasConstant.KE_RPC_COOKIE, param);
            BaseResult<Boolean> result = parseAcceptResult(str);
            return result;
        } catch (Exception e) {
            String message = e.getMessage().substring(e.getMessage().lastIndexOf("{"), e.getMessage().lastIndexOf("}") + 1);
            JSONObject j = JSONObject.parseObject(message);
            if ("71".equals(j.getString("error_code"))) return BaseResult.fail("银行通信中断，受理失败");
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * lastCount接口token验证
     */
    private boolean checkToken(String account, String token) throws NoSuchAlgorithmException {
        Assert.hasLength(account, "账号不能为空");
        Assert.hasLength(token, "token不能为空");
        //token验证
        JSONObject obj = new JSONObject();
        obj.put("clientId", YmlConfig.msClientId);
        obj.put("account", account);
        obj.put("code", YmlConfig.msCode);
        String str = obj.toJSONString();
        MessageDigest md = MessageDigest.getInstance("MD5");
        String dis = InfoUtils.byteArrToHex(md.digest(str.getBytes(StandardCharsets.UTF_8)));
        if (dis.equals(dis)) {
            return true;
        }
        return false;
    }

    private BaseResult<AlarmCount> parseCountResult(String countStr) {
        JSONObject countObj = JSONObject.parseObject(countStr);
        String countCode = countObj.getString("error_code");
        if (!"00".equals(countCode)) {
            return BaseResult.fail("查询历史告警数量失败！");
        }
        String countData = countObj.getString("data");
        AlarmCount alarmCount = JSON.parseObject(countData, AlarmCount.class);
        return BaseResult.success(alarmCount);
    }

    private BaseResult<Boolean> parseAcceptResult(String str) {
        JSONObject obj = JSONObject.parseObject(str);
        String code = obj.getString("error_code");
        if (!"00".equals(code)) {
            String msg = obj.getString("error_msg");
            return BaseResult.fail("告警操作失败！" + msg);
        }
        //String data = obj.getString("data");
        return BaseResult.success(true);
    }

}

