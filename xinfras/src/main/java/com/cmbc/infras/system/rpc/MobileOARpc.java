package com.cmbc.infras.system.rpc;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 移动OA接口
 */
@FeignClient(name = "mobile", url = "${mobile.oa.server}")
public interface MobileOARpc {

    /**
     * 单发即时通待办消息，或邮件
     */
    @PostMapping("/sqs/api/queue/restSendUnifyMessage")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String restSendUnifyMessage(Object param);

    /**
     * 单发即时通待办消息，或邮件（经过服务认证，携带token）
     */
    @PostMapping("/sqs/api/queue/restSendUnifyMessage")
    @Headers({"Content-Type: application/json", "Accept: application/json"})
    String restSendUnifyMessageWithToken(@RequestHeader("Authorization") String Authorization, Object param);

}
