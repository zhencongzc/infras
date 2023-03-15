package com.cmbc.infras.system.rpc;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.health.AlarmRequestParam;
import com.cmbc.infras.dto.health.FormRequestParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 流程表单远程调用
 */
@FeignClient(name = "flowForm", url = "${ke-rpc.server}")
public interface FlowFormRpc {

    /**
     * 获取列表数据
     */
    @PostMapping("/api/flow/api/v1/bfm/instances/table/list")
    String getFormData(@RequestHeader("token") String token, @RequestBody JSONObject param);

    /**
     * 获取表单数据-”基础设施“
     */
    @PostMapping("/api/flow/api/v1/bfm/instances/model/list")
    String getInfrastructureData(@RequestHeader("token") String token, @RequestBody FormRequestParam param);

    /**
     * 获取告警信息
     */
    @PostMapping("/api/v2/tsdb/status/event")
    String getAlarmData(@RequestHeader("Cookie") String cookie, AlarmRequestParam param);

    /**
     * 获取表单所有状态
     */
    @GetMapping("/api/flow/api/v1/bfm/dictionary/getDictionary")
    String getFormState(@RequestHeader("Cookie") String cookie, @RequestParam("moduleResourceId") String moduleResourceId,
                        @RequestParam("elementId") String elementId);

    /**
     * 获取所有表单
     */
    @GetMapping("/api/flow/api/v1/bfm/config/data/module/list")
    String getFormList(@RequestHeader("Cookie") String cookie);
}
