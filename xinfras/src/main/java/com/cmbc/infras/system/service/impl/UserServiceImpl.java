package com.cmbc.infras.system.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.DataConfigMapper;
import com.cmbc.infras.system.service.UserService;
import com.cmbc.infras.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private DataConfigMapper dataConfigMapper;

//    @Override
//    public BaseResult<UserDto> loginProcess(HttpServletRequest request, HttpServletResponse response, String code) {
//
//        StringBuffer url = new StringBuffer(MobileAuthConfig.tokenUrl)
//                .append("?client_id=").append(MobileAuthConfig.clientId)
//                .append("&client_secret=").append(MobileAuthConfig.secret)
//                .append("&grant_type=authorization_code")
//                .append("&redirect_uri=").append(MobileAuthConfig.redirectUrl)
//                .append("&code=").append(code);
//        log.info("移动OA统一认证...to get token url:{}", url.toString());
//        HttpRequest post = HttpUtil.createPost(url.toString());
//        HttpResponse tokenResponse = post.execute();
//        log.info("移动OA统一认证...to get token result status:{}, body:{}", tokenResponse.getStatus(), tokenResponse.body());
//
//        if (HttpStatus.HTTP_OK != tokenResponse.getStatus()) {
//            log.error("移动OA统一认证...to get token response_body:{}", tokenResponse.body());
//            return BaseResult.fail("OA统一认证getToken异常!");
//        }
//
//        String body = tokenResponse.body();
//        ResponseToken responseToken = JSON.parseObject(body, ResponseToken.class);
//        //移动OA统一认证token换取用户信息
//        BaseResult<TokenUser> userResult = getUserByToken(responseToken.accessToken);
//        if (!userResult.isSuccess()) {
//            return BaseResult.fail(userResult.getMessage());
//        }
//
//        //{"userId": "wangyongjun1","userNumber": "0000001295"}这里用userId
//        TokenUser tUser = userResult.getData();
//        String account = tUser.userId;
//        //account登录KE,门户-请求页面设置cookie
//        BaseResult<UserDto> loginResult = login(response, account);
//        return loginResult;
//    }

    private BaseResult<TokenUser> getUserByToken(String accessToken) {
        StringBuffer url = new StringBuffer(MobileAuthConfig.userInfoUrl).append("?accesstoken=").append(accessToken);
        log.info("移动OA统一认证-请求用户信息 accessToken:{}, url:{}", accessToken, url.toString());
        HttpRequest post = HttpUtil.createPost(url.toString());
        HttpResponse response = post.execute();
        log.info("移动OA统一认证-请求用户信息 result status:{}, body:{}", response.getStatus(), response.body());
        if (response.getStatus() == cn.hutool.http.HttpStatus.HTTP_OK) {
            String body = response.body();
            TokenUser user = JSON.parseObject(body, TokenUser.class);
            return BaseResult.success(user);
        } else {
            log.error("移动OA统一认证-请求用户信息 error! url:{}, response status:{}, body:{}", url.toString(), response.getStatus(), response.body());
            return BaseResult.fail("移动OA统一认证-请求用户信息异常!");
        }
    }

    /**
     * 登录KE,门户
     * 设置页面cookie
     * 设置成功跳转路径(默认门户首页)
     */
    @Override
    public BaseResult<UserDto> login(HttpServletResponse response, String account) {
        //登录KE
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("account", account);
        jsonObject.put("time", (System.currentTimeMillis() / 1000));
        jsonObject.put("url", "");
        KeEncryptUtil keEncryptUtil = new KeEncryptUtil(YmlConfig.keEncrtyKey);
        String token = keEncryptUtil.encrypt(jsonObject.toJSONString());
        StringBuffer url = new StringBuffer(YmlConfig.keUrl).append("/api/v3/xsso/token/check?token=").append(token);
        log.info("移动OA统一认证-登录KE url:{}", url.toString());
        HttpRequest get = HttpUtil.createGet(url.toString());
        String rbody = get.execute().body();
        log.info("移动OA统一认证-登录KE result body:{}", rbody);
        //登录门户
        JSONObject kebody = JSON.parseObject(rbody);
        if (kebody.getInteger("error_code") != 200) {
            log.error("移动OA统一认证-登录KE 异常 body:{}", rbody);
            return BaseResult.fail("移动OA统一认证-登录KE 异常");
        }
        StringBuffer psb = new StringBuffer(YmlConfig.simpleAdminUrl).append("/auth/sso/login");
        log.info("移动OA统一认证-登录门户 url:{}", psb.toString());
        HttpRequest post = HttpUtil.createPost(psb.toString());
        post.body(kebody.getString("data"));
        String pbody = post.execute().body();
        log.info("移动OA统一认证-登录门户 result body:" + pbody);
        JSONObject sbody = JSONObject.parseObject(pbody);
        if (sbody.getInteger("code") != 200) {
            log.error("移动OA统一认证-登录门户 异常 body:{}", pbody);
            return BaseResult.fail("移动OA统一认证-登录门户 异常");
        }
        JSONObject userInfoData = sbody.getJSONObject("data").getJSONObject("user");
        String keToken = sbody.getJSONObject("data").getString("token");

        /**
         * 请求页面设置cookie
         */
        setLoginCookie(response, userInfoData, keToken, account);
        UserDto userDto = JSONObject.parseObject(userInfoData.toJSONString(), UserDto.class);

        //登录成功跳转页-默认门户
        userDto.setRedirectUrl("/portal");
        userDto.setToken(keToken);
        return BaseResult.success(userDto);
    }

    private void setLoginCookie(HttpServletResponse response, JSONObject userInfoData, String token, String account) {
        String path = "/";
        Cookie sidCookie = new Cookie(CookieKey.SESSION_ID, userInfoData.getString("sessionId"));
        sidCookie.setPath(path);
        response.addCookie(sidCookie);
        Cookie authTokenCookie = new Cookie(CookieKey.AUTH_TOKEN, token);
        authTokenCookie.setPath(path);
        response.addCookie(authTokenCookie);
        //设置BANK_ID的cookie
        String redisKey = InfrasConstant.AUTH_LOGIN + token;
        UserDto user = DataRedisUtil.getStringFromRedis(redisKey, UserDto.class);
        log.info("setLoginCookie: redisKey: {},user: {}", redisKey, user);
        if (user != null) {
            String sessionId = user.getSessionId();
            //1从流程引擎获取银行名称
            String bankName = AccountBankUtil.getAccountBankName(account, sessionId);
            if ("机房管理中心".equals(bankName)) bankName = "中国民生银行";
            //2从数据库查询银行id
            List<JSONObject> bank = dataConfigMapper.findBankByName(bankName);
            if (bank.size() != 0) {
                String bankId = bank.get(0).getString("bankId");
                Cookie bankCookie = new Cookie(CookieKey.USER_BANK_ID, bankId);
                bankCookie.setPath(path);
                response.addCookie(bankCookie);
                log.info("setLoginCookie: bankId: {},bankCookie: {}", bankId, bankCookie);
            }
        }
        try {
            BASE64Encoder encoder = new BASE64Encoder();
            Cookie nameCookie = new Cookie(CookieKey.USER_NAME, encoder.encode(userInfoData.getString("name").getBytes("UTF-8")));
            nameCookie.setPath(path);
            response.addCookie(nameCookie);
            Cookie accountCookie = new Cookie(CookieKey.ACCOUNT, encoder.encode(userInfoData.getString("account").getBytes("UTF-8")));
            accountCookie.setPath(path);
            response.addCookie(accountCookie);
        } catch (Exception e) {
            log.error("设置全局缓存出错", e);
        }
        Cookie themeCookie = new Cookie(CookieKey.THEME, userInfoData.getString("theme").trim());
        themeCookie.setPath(path);
        response.addCookie(themeCookie);
        Cookie langCookie = new Cookie(CookieKey.LOCAL_LANG, "zh-CN");
        langCookie.setPath(path);
        response.addCookie(langCookie);
        Cookie idCookie = new Cookie(CookieKey.USER_ID, userInfoData.getString("id"));
        idCookie.setPath(path);
        response.addCookie(idCookie);
        Cookie modeCookie = new Cookie(CookieKey.MODE, "0");
        modeCookie.setPath(path);
        response.addCookie(modeCookie);
        Cookie productCookie = new Cookie(CookieKey.X_PRODUCT, "gu");
        productCookie.setPath(path);
        response.addCookie(productCookie);
        Cookie userCookie = new Cookie(CookieKey.USER_INFO, AesUtil.encrypt(userInfoData.toJSONString()));
        userCookie.setPath(path);
        response.addCookie(userCookie);
        Cookie wsCookie = new Cookie(CookieKey.WS_ACCOUNT, account);
        wsCookie.setPath(path);
        response.addCookie(wsCookie);
    }

    /**
     * 民生银行返回token信息
     */
    static class ResponseToken {
        public int expiresIn;
        public String accessToken;
        public String refreshToken;
    }

    /**
     * 通过accessToken
     * 查询User信息
     */
    static class TokenUser {
        public String userId;
        public String userNumber;
    }
}
