package com.cmbc.infras.system.controller;

import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.system.service.WorkBenchService;
import com.cmbc.infras.system.vo.AlarmKpiVo;
import com.cmbc.infras.system.vo.BankKpiVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 生产运营工作台相关接口
 */
@Slf4j
@RestController
@RequestMapping("/workBench")
public class WorkBenchController {
    @Autowired
    private WorkBenchService workBenchService;

    /**
     * 告警指标
     * 做缓存，日统计5分钟更新一次，周统计60分钟更新一次
     */
    @RequestMapping("/alarmKPI")
    public BaseResult<List<AlarmKpiVo>> getAlarmKPI(@RequestParam(defaultValue = "day") String type) {
        try {
            List<AlarmKpiVo> result = workBenchService.getAlarmKpiByType(type);
            return BaseResult.success(result);
        } catch (Exception e) {
            return BaseResult.fail("获取告警指标异常");
        }
    }

    /**
     * 银行告警指标
     * 做缓存，日统计5分钟更新一次，周统计60分钟更新一次
     */
    @RequestMapping("/bankKPI")
    public BaseResult<List<BankKpiVo>> getBankKpiList(@RequestParam(defaultValue = "day") String type) {
        try {
            List<BankKpiVo> result = workBenchService.getBankKpiByType(type);
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("getBankKpiByType : {}", e.getMessage(), e);
            return BaseResult.fail("获取银行告警指标异常");
        }
    }
}
