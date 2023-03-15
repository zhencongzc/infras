package com.cmbc.infras.health.config;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.util.KeEncryptUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * 拦截Feign请求添加Header-停用
 */
//@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private static Logger logger = Logger.getLogger(FeignRequestInterceptor.class.getName());

    @Override
    public void apply(RequestTemplate requestTemplate) {
        /**
         * 登录KE门户获取sessionId
         */
        //登录ke
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("account", "admin");
        jsonObject.put("time", (System.currentTimeMillis() / 1000));
        jsonObject.put("url", "");
        KeEncryptUtil keEncryptUtil = new KeEncryptUtil(YmlConfig.keEncrtyKey);
        String token = keEncryptUtil.encrypt(jsonObject.toJSONString());
        String url = YmlConfig.keUrl + "/api/v3/xsso/token/check?token=" + token;
        HttpRequest get = HttpUtil.createGet(url);
        String execute = get.execute().body();
        logger.info("请求接口/api/v3/xsso/token/check 返回参数execute=" + execute);
        JSONObject executeObj = JSON.parseObject(execute);
        Integer code = executeObj.getInteger("error_code");
        if (code != 200) logger.warning("后台admin登录KE失败！result:{}");
        //登录门户
        HttpRequest post = HttpUtil.createPost(YmlConfig.simpleAdminUrl + "/auth/sso/login");
        post.body(executeObj.getString("data"));
        logger.info("请求接口/auth/sso/login 入参body=" + executeObj.getString("data"));
        String body = post.execute().body();
        logger.info("请求接口/auth/sso/login 返回参数body=" + body);
        JSONObject simpleAdminLoginObj = JSONObject.parseObject(body);
        code = simpleAdminLoginObj.getInteger("code");
        if (code != 200) logger.warning("后台admin登录门户失败!result:{}");
        JSONObject userInfoData = simpleAdminLoginObj.getJSONObject("data").getJSONObject("user");
        String sessionId = userInfoData.getString("sessionId");
        requestTemplate.header("token", sessionId);//读配置
    }
}
