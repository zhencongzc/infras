package com.cmbc.infras.system.rpc;

import com.cmbc.infras.dto.rpc.MonitorRpcParam;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 查询银行心跳
 * KE-工程组态-连接视图
 */
@FeignClient(name = "beat", url = "${ke-rpc.server}")
public interface HeartBeatRpc {

    @PostMapping("/api/v2/tsdb/status/last")
    @Headers({"Content-Type: application/json","Accept: application/json"})
    String getHeartBeat(@RequestHeader("Cookie") String cookie, MonitorRpcParam param);

}
