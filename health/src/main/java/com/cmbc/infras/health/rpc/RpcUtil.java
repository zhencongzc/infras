package com.cmbc.infras.health.rpc;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.health.config.YmlConfig;
import com.cmbc.infras.util.KeEncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

@Slf4j
public class RpcUtil {

    /**
     * 从前端获取cookie
     */
    public static String getCookie() {
        StringBuilder cookie = new StringBuilder();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        cookie.append(c.getName()).append("=").append(c.getValue()).append(";");
                    }
                }
            }
        }
        return cookie.toString();
    }

    /**
     * 登录KE门户获取token
     */
    public static String getToken() {
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
        log.info("请求KE接口/api/v3/xsso/token/check 返回参数execute=" + execute);
        JSONObject executeObj = JSON.parseObject(execute);
        Integer code = executeObj.getInteger("error_code");
        if (code != 200) log.warn("后台admin登录KE失败！result:{}");
        //登录门户
        HttpRequest post = HttpUtil.createPost(YmlConfig.simpleAdminUrl + "/auth/sso/login");
        post.body(executeObj.getString("data"));
        log.info("请求门户接口/api/admin/auth/sso/login 入参body=" + executeObj.getString("data"));
        String body = post.execute().body();
        log.info("请求门户接口/api/admin/auth/sso/login 返回参数body=" + body);
        JSONObject simpleAdminLoginObj = JSONObject.parseObject(body);
        code = simpleAdminLoginObj.getInteger("code");
        if (code != 200) log.warn("后台admin登录门户失败!result:{}");
        JSONObject userInfoData = simpleAdminLoginObj.getJSONObject("data").getJSONObject("user");
        String sessionId = userInfoData.getString("sessionId");
        return sessionId;
    }
}
