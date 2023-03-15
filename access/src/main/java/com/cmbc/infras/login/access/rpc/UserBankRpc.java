package com.cmbc.infras.login.access.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "${bankRpc.name}")
public interface UserBankRpc {

    @GetMapping("${bankRpc.api}")
    String getUserBankId(@RequestParam("account") String account);

}
