package com.cmbc.infras.system.rpc;


import com.cmbc.infras.dto.rpc.alarm.WhereCountIterm;
import com.cmbc.infras.dto.rpc.alarm.WhereDataItem;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "alarm", url = "${ke-rpc.server}")
public interface HistoryAlarmRpc {

    /**
     * 历史告警数量,数据查询
     * 替换EventRpc中实现
     */
    @PostMapping("/api/v2/tsdb/status/event/count")
    @Headers({"Content-Type: application/json","Accept: application/json"})
    String getHistoryAlarmCount(@RequestHeader("Cookie") String cookie, WhereCountIterm param);

    @PostMapping("/api/v2/tsdb/status/event")
    @Headers({"Content-Type: application/json","Accept: application/json"})
    String getHistoryAlarm(@RequestHeader("Cookie") String cookie, WhereDataItem param);

}
