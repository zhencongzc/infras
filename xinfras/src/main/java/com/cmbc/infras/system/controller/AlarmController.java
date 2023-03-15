package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.Label;
import com.cmbc.infras.dto.LabelParam;
import com.cmbc.infras.dto.event.AlarmCount;
import com.cmbc.infras.dto.rpc.event.*;
import com.cmbc.infras.system.service.AlarmService;
import com.cmbc.infras.system.service.EventService;
import com.cmbc.infras.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * 提供给三方部门-告警页面专用
 * 打开页面先全量查询实时告警-admin权限
 * WebSocket推送-WebSocketServer
 * 查询历史告警
 * 受理功能
 */
@RestController
@RequestMapping("/alarm")
@Slf4j
public class AlarmController {

    @Resource
    private AlarmService alarmService;
    @Resource
    private EventService eventService;

    /**
     * 三方告警页-取code
     */
    @RequestMapping("/authCode")
    public BaseResult<String> getAuthCode() {
        String authCode = UserContext.getAuthToken();
        if (StringUtils.isBlank(authCode)) return BaseResult.fail("账号未登录");
        return BaseResult.success(authCode);
    }

    /**
     * 实时告警列表总数
     * 生产运营工作台嵌入frame告警信息
     * 单独查数量接口-权限放开-不通过统一认证
     */
    @RequestMapping("/lastCount")
    public BaseResult<Integer> getAllAlarmCount(String account, String token) {
        try {
            Integer count = eventService.getAllAlarmCount(account, token);
            return BaseResult.success(count);
        } catch (Exception e) {
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 实时告警查询（分行运维-小铃铛）
     */
    @RequestMapping("/last")
    public BaseResult<List<AlarmEvent>> getAllAlarms(@RequestBody JSONObject param) {
        BaseResult<List<AlarmEvent>> allAlarm = alarmService.getAllAlarms(param, false);
        return allAlarm;
    }

    /**
     * 历史告警查询（告警-三个点）
     * 带分页
     */
    @RequestMapping("/history")
    public BaseResult<List<AlarmEvent>> getHistoryAlarm(@RequestBody JSONObject param) {
        return alarmService.getHistoryAlarm(param);
    }

    /**
     * 实时告警数量查询（分行运维-小铃铛）
     * 紧急-严重-重要-次要-预警
     */
    @RequestMapping("/count")
    public BaseResult<AlarmCount> getAlarmCount(@RequestParam(required = false) String bankId) {
        return eventService.getEventLastCount(bankId);
    }

    /**
     * 告警受理
     */
    @RequestMapping("/accept")
    public BaseResult<Boolean> alarmAccept(@RequestBody AlarmAcceptParam param) {
        return alarmService.alarmAccept(param);
    }

    /**
     * 告警确认
     */
    @RequestMapping("/confirm")
    public BaseResult<Boolean> alarmConfirm(@RequestBody AlarmConfirmParam param) {
        return alarmService.alarmConfirm(param, false);
    }

    /**
     * 查询所有标签
     */
    @RequestMapping("/labels")
    public BaseResult<List<Label>> getLabels() {
        return alarmService.getLabels();
    }

    /**
     * 根据ID查询标签
     */
    @RequestMapping("/label")
    public BaseResult<Label> getLabel(Integer id) {
        Assert.notNull(id, "标签ID不能为空！");
        return alarmService.getLabel(id);
    }

    /**
     * 标签选中接口
     */
    @RequestMapping("/labelCheck")
    public BaseResult<Boolean> labelCheck(Integer id) {
        Assert.notNull(id, "标签ID不能为空！");
        return alarmService.labelCheck(id);
    }

    /**
     * 添加标签
     */
    @RequestMapping("/addLabel")
    public BaseResult<Label> addLabel(@RequestBody LabelParam label) {
        return alarmService.addLabel(label);
    }

    /**
     * 修改标签
     */
    @RequestMapping("/editLabel")
    public BaseResult<Boolean> editLabel(@RequestBody LabelParam label) {
        return alarmService.editLabel(label);
    }

    /**
     * 删除标签
     */
    @RequestMapping("/delLabel")
    public BaseResult<Boolean> delLabel(Integer id) {
        Assert.notNull(id, "标签ID不能为空！");
        return alarmService.delLabel(id);
    }

    /**
     * 查询40家分行-位置
     * 不需要参数,level=1
     */
    @RequestMapping("/locations")
    public BaseResult<List<Bank>> getLocations() {
        return alarmService.getLocations();
    }

    /**
     * 快捷输入查询
     */
    @PostMapping("/fastInput/find")
    public BaseResult<List<JSONObject>> findFastInput(@RequestBody JSONObject param) {
        List<JSONObject> res = alarmService.findFastInput(param.getIntValue("type"));
        return BaseResult.success(res);
    }

    /**
     * 快捷输入新增
     */
    @PostMapping("/fastInput/add")
    public BaseResult<Integer> addFastInput(@RequestBody JSONObject param) {
        int id = alarmService.addFastInput(param.getIntValue("type"), param.getString("content"));
        return BaseResult.success(id);
    }

    /**
     * 快捷输入编辑
     */
    @PostMapping("/fastInput/update")
    public BaseResult<String> updateFastInput(@RequestBody JSONObject param) {
        alarmService.updateFastInput(param.getIntValue("id"), param.getString("content"));
        return BaseResult.success("");
    }

    /**
     * 快捷输入删除
     */
    @PostMapping("/fastInput/delete")
    public BaseResult<String> deleteFastInput(@RequestBody JSONObject param) {
        alarmService.deleteFastInput(param.getIntValue("id"));
        return BaseResult.success("");
    }

}
