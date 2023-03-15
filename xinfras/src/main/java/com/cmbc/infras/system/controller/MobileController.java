package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.AirStateEnum;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseParam;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.event.HistoryAlarmParam;
import com.cmbc.infras.dto.rpc.event.AlarmAcceptParam;
import com.cmbc.infras.dto.rpc.event.AlarmConfirmParam;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.system.service.HistoryAlarmService;
import com.cmbc.infras.system.service.MobileService;
import com.cmbc.infras.system.util.BusinessUtil;
import com.cmbc.infras.system.util.TransferUtil;
import com.cmbc.infras.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 移动端
 */
@Slf4j
@RestController
@RequestMapping("/mobile")
public class MobileController {

    @Resource
    private FlowFormService flowFormService;
    @Resource
    private MobileService mobileService;
    @Resource
    private HistoryAlarmService historyAlarmService;

    /**
     * 获取下级银行
     */
    @RequestMapping("/banks")
    public BaseResult<List<Bank>> getBanks(BaseParam param) {
        BaseResult<List<Bank>> banks = mobileService.getBanks(param);
        return banks;
    }

    /**
     * 活动告警-实时告警
     */
    @RequestMapping("/alarms")
    public BaseResult<List<AlarmEvent>> getAlarms(@RequestBody JSONObject param) {
        String bankId = param.getString("bankId");
        //标签筛选
        Label label = new Label();
        label.setId(param.getIntValue("id"));
        label.setEventLevel(param.getString("eventLevel"));
        label.setRecoverState(param.getString("recoverState"));//恢复状态
        label.setProcessState(param.getString("processState"));//处理状态
        String sessionId = UserContext.getAuthToken();
        List<AlarmEvent> res = mobileService.getAlarms(label).getData();
        //带银行id则进行过滤，筛选当前银行及下属银行的数据
        if (StringUtils.isNotBlank(bankId)) {
            List<String> bankIds = flowFormService.getCacheSubBankIds(bankId, sessionId);
            bankIds.add(bankId);
            Iterator<AlarmEvent> iterator = res.iterator();
            while (iterator.hasNext()) {
                //删除不匹配的数据
                if (!BusinessUtil.alarmMatchBankId(iterator.next(), bankIds)) iterator.remove();
            }
            log.info("实时告警进行了筛选，分行id：{}，下属银行ids：{}", bankId, bankIds);
        }
        //设置告警类型，阈值
        for (AlarmEvent event : res) {
            event.setEventTypeName(AirStateEnum.getDescription(event.getEventType()));//告警类型
            int i = event.getGuid().lastIndexOf("_");
            event.setThreshold(i == -1 ? "" : event.getGuid().substring(i + 1));//阈值
        }
        return BaseResult.success(res, res.size());
    }

    /**
     * 查询下级分行
     */
    @GetMapping("/getSubBanks")
    public BaseResult<List<Bank>> getSubBanks() {
        List<Bank> res = mobileService.getSubBanks();
        return BaseResult.success(res, res.size());
    }

    /**
     * 分行设备信息-首页用
     * 只展示A分，B分
     */
    @PostMapping("/upss")
    public BaseResult<List<JSONObject>> upss(@RequestBody JSONObject param) {
        try {
            BaseResult<List<JSONObject>> upss = mobileService.upss(param.getJSONArray("list").toJavaList(Bank.class));
            return upss;
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 分行告警数量查询
     */
    @PostMapping("/alarmCount")
    public BaseResult<HashMap<String, Integer>> alarmCount(@RequestBody JSONObject param) {
        try {
            HashMap<String, Integer> res = mobileService.alarmCount(param);
            return BaseResult.success(res,res.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 分行设备信息-分行及下级用（带参数）
     * 只展示A分、B分
     * 只展示当前银行
     */
    @GetMapping("/getUpss")
    public BaseResult<List<JSONObject>> getUpss(String bankId) {
        try {
            List<JSONObject> data = mobileService.getUpss(bankId);
            return BaseResult.success(data, data.size());
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 历史告警
     * 根据当前用户标签过滤告警等级，分页查询全部最近一个月告警
     */
    @RequestMapping("/historyAlarms")
    public BaseResult<List<AlarmEvent>> getHistoryAlarms(@RequestBody HistoryAlarmParam haParam) {
        String sessionId = UserContext.getAuthToken();
        String bankId = haParam.getBankId();
        if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
        List<String> bankIds = flowFormService.getCacheSubBankIds(bankId, sessionId);
        bankIds.add(bankId);
        AlarmCount alarmCount = historyAlarmService.getHistoryAlarmCount(bankIds);
        List<Event> events = historyAlarmService.getHistoryAlarmData(bankIds, haParam.getNumber(), haParam.getSize());
        List<AlarmEvent> alarms = TransferUtil.eventsToAlarms(events);
        return new BaseResult(true, "", alarms, alarmCount.getCount(), haParam.getSize(), haParam.getNumber());
    }

    /**
     * 告警确认
     */
    @RequestMapping("/confirm")
    public BaseResult<Boolean> confirm(@RequestBody AlarmConfirmParam param) {
        BaseResult<Boolean> confirm = mobileService.confirm(param);
        return confirm;
    }

    /**
     * 告警受理
     */
    @RequestMapping("/accept")
    public BaseResult<Boolean> accept(@RequestBody AlarmAcceptParam param) {
        BaseResult<Boolean> accept = mobileService.accept(param);
        return accept;

    }

}
