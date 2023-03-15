package com.cmbc.infras.system.rpc;

import com.cmbc.infras.dto.rpc.MonitorRpcParam;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "monitor", url = "${ke-rpc.server}")
public interface MonitorRpc {

    @PostMapping("/api/v2/tsdb/status/last")
    @Headers({"Content-Type: application/json","Accept: application/json"})
    String getMonitorList(@RequestHeader("Cookie") String cookie, MonitorRpcParam param);

}
