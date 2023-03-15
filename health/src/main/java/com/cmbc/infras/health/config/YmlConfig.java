package com.cmbc.infras.health.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class YmlConfig {

    public static String keUrl;//ke接口地址
    public static String keEncrtyKey;//ke加密的key
    public static String simpleAdminUrl;//门户地址

    @Value("${ke-rpc.server}")
    public void setKeUrl(String keUrlValue) {
        keUrl = keUrlValue;
    }

    @Value("${ke.encrtyKey}")
    public void setKeEncrtyKey(String keEncrtyKeyValue) {
        keEncrtyKey = keEncrtyKeyValue;
    }

    @Value("${simple.admin.url}")
    public void setSimpleAdminUrl(String simpleAdminUrlValue) {
        simpleAdminUrl = simpleAdminUrlValue;
    }

}
