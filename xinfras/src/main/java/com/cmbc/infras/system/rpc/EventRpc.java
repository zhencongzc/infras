package com.cmbc.infras.system.rpc;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.rpc.SpotValDto;
import com.cmbc.infras.dto.rpc.event.AlarmAcceptParam;
import com.cmbc.infras.dto.rpc.event.AlarmConfirmParam;
import com.cmbc.infras.dto.rpc.event.EventGroupParam;
import com.cmbc.infras.dto.rpc.event.EventParam;
import com.cmbc.infras.dto.rpc.spot.SpotDto;
import com.cmbc.infras.system.exception.MyFallbackFactory;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * KE远程调用
 */
@FeignClient(name = "event", url = "${ke-rpc.server}", fallbackFactory = MyFallbackFactory.class)
public interface EventRpc {

    /**
     * 实时告警数量查询
     */
    @PostMapping("/api/v3/tsdb/status/event/last/count")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEventLastCount(@RequestHeader("Cookie") String cookie, EventGroupParam param);

    /**
     * 实时告警数量查询
     */
    @PostMapping("/api/v3/tsdb/status/event/last/count")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEventLastCount(@RequestHeader("Cookie") String cookie, JSONObject param);

    /**
     * 实时告警查询
     */
    @PostMapping("/api/v2/tsdb/status/event/last")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEventLast(@RequestHeader("Cookie") String cookie, EventParam param);

    /**
     * 历史告警查询
     */
    @PostMapping("/api/v2/tsdb/status/event/count")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEventCount(@RequestHeader("Cookie") String cookie, EventParam param);

    /**
     * 历史告警查询
     */
    @PostMapping("/api/v2/tsdb/status/event/count")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEventCount(@RequestHeader("Cookie") String cookie, JSONObject param);

    @PostMapping("/api/v2/tsdb/status/event")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEvent(@RequestHeader("Cookie") String cookie, EventParam param);

    @PostMapping("/api/v2/tsdb/status/event")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEvent(@RequestHeader("Cookie") String cookie, JSONObject param);

    /**
     * 告警受理
     */
    @PostMapping("/api/v2/tsdb/status/event/accept")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String alarmAccept(@RequestHeader("Cookie") String cookie, AlarmAcceptParam param);

    /**
     * 告警确认
     */
    @PostMapping("/api/v2/tsdb/status/event/confirm")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String alarmConfirm(@RequestHeader("Cookie") String cookie, AlarmConfirmParam param);

    /**
     * 工程组态-空间视图
     */
    @GetMapping("/api/v2/cmdb/resources/relations")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String spaceView(@RequestHeader("Cookie") String cookie, @RequestParam("resource_id") String resource_id,
                     @RequestParam("attribute_name") String attribute_name, @RequestParam("attribute_value") String attribute_value,
                     @RequestParam("relation_code") String relation_code, @RequestParam("output_format") String output_format);

    /**
     * 工程组态-设备测点列表
     */
    @PostMapping("/api/v2/cmdb/resources/relations")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String spaceDeviceSpotList(@RequestHeader("Cookie") String cookie, SpotDto spotDto);

    /**
     * 获取测点实时值
     */
    @PostMapping("/api/v2/tsdb/status/last")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getSpotLast(@RequestHeader("Cookie") String cookie, SpotValDto param);

    /**
     * 查询角色列表（系统管理-权限管理-角色管理）
     */
    @GetMapping("/api/v2/auth/role/list")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getRoleList(@RequestHeader("Cookie") String cookie);

    /**
     * 查询用户列表（系统管理-权限管理-用户管理）
     */
    @GetMapping("/api/v2/auth/employee/list")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getEmployeeList(@RequestHeader("Cookie") String cookie);

    /**
     * 查询角色详情（系统管理-权限管理-角色管理）
     */
    @GetMapping("/api/v2/auth/role/employees")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String getRolesDetail(@RequestHeader("Cookie") String cookie, @RequestParam("id") Integer id);

    /**
     * 资产管理更新数据
     */
    @PostMapping("/dcim/overviewController/asset/update")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String sendAsset(@RequestHeader("Cookie") String cookie, JSONObject param);

    /**
     * 获取组织结构和角色信息
     */
    @PostMapping("/api/admin/rpc/dept/getTreeWithUser")
    String getOrganizationAndRole(@RequestBody JSONObject param);
}
