package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.monitor.AirInfo;
import com.cmbc.infras.dto.monitor.Humidity;
import com.cmbc.infras.dto.monitor.UpsInfo;
import com.cmbc.infras.system.service.MonitorService;
import com.cmbc.infras.util.UserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class MonitorController {

    @Resource
    private MonitorService monitorService;

    /**
     * UPS状态展示（分行运维）
     */
    @RequestMapping("/monitor/upss")
    public BaseResult<JSONObject> upss(String bankId) {
        try {
            //分行登录从cookie获取银行id
            if (StringUtils.isBlank(bankId)) bankId = UserContext.getUserBankId();
            return monitorService.upss(bankId);
        } catch (Exception e) {
            e.printStackTrace();
            return BaseResult.fail(e.getMessage());
        }
    }

    /**
     * 空调状态展示（分行运维）
     */
    @RequestMapping("/monitor/airs")
    public BaseResult<List<AirInfo>> airs(String bankId) {
        return monitorService.getAirs(bankId);
    }

    /**
     * 温湿度状态（分行运维）
     */
    @RequestMapping("/monitor/humids")
    public BaseResult<List<Humidity>> humids(String bankId) {
        return monitorService.getHumids(bankId);
    }

    /**
     * 更多（移动端-下属行信息）
     */
    @PostMapping("/monitor/childBankData")
    public BaseResult<List<JSONObject>> childBankData(@RequestBody JSONObject param) {
        List<JSONObject> result = monitorService.childBankData(param.getString("bankId"));
        return BaseResult.success(result);
    }

}
