package com.cmbc.infras.system.service;

import com.cmbc.infras.dto.BaseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "sys-system")
public interface UserRpc {

    @GetMapping("/admin/getAdminPassword")
    BaseResult<String> getAdminPassword(String name);
}


