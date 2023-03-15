package com.cmbc.infras.health.rpc;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.health.dto.AlarmRequestParam;
import com.cmbc.infras.health.dto.FormRequestParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 流程引擎信息查询
 */
@FeignClient(name = "process", url = "${pe-rpc.server}")
public interface ProcessEngineRpc {

    /**
     * 获取组织结构和角色信息
     */
    @PostMapping("/api/admin/rpc/dept/getTreeWithUser")
    String getOrganizationAndRole(@RequestBody JSONObject param);

    /**
     * 获取所有表单
     */
    @GetMapping("/api/flow/api/v1/bfm/config/data/module/list")
    String getFormList(@RequestHeader("Cookie") String cookie);

    /**
     * 获取表单所有状态
     */
    @GetMapping("/api/flow/api/v1/bfm/dictionary/getDictionary")
    String getFormState(@RequestHeader("Cookie") String cookie, @RequestParam("moduleResourceId") String moduleResourceId,
                        @RequestParam("elementId") String elementId);

    /**
     * 获取表单数据
     */
    @PostMapping("/api/flow/api/v1/bfm/instances/model/list")
    String getFormData(@RequestHeader("Cookie") String cookie, @RequestBody JSONObject param);

    /**
     * 获取表单数据（使用token）
     */
    @PostMapping("/api/flow/api/v1/bfm/instances/model/list")
    String getFormDataByToken(@RequestHeader("token") String token, @RequestBody JSONObject param);

    /**
     * 获取表单数据-”基础设施“
     */
    @PostMapping("/api/flow/api/v1/bfm/instances/model/list")
    String getInfrastructureData(@RequestHeader("Cookie") String cookie, @RequestBody FormRequestParam param);

//    /**
//     * 获取角色组织信息
//     */
//    @PostMapping("/api/admin/rpc/user/getUserById")
//    String getRoleOrganization(@RequestBody JSONObject param);

    /**
     * 获取测点数据
     */
    @PostMapping("/api/v2/tsdb/status/last")
    String getSpotData(@RequestHeader("Cookie") String cookie, JSONObject param);

    /**
     * 获取告警信息
     */
    @PostMapping("/api/v2/tsdb/status/event")
    String getAlarmData(@RequestHeader("Cookie") String cookie, AlarmRequestParam param);

    /**
     * 报表报告-数据查询
     * check为body数据md5加密后的值
     */
    @PostMapping("/api/v3/tsdb/orig/query_agg")
    String findData(@RequestHeader("Cookie") String cookie, @RequestHeader("check") String check, String param);

}
