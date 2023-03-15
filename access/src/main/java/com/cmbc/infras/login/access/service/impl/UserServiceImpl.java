package com.cmbc.infras.login.access.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.login.access.mapper.UsualMapper;
import com.cmbc.infras.login.access.service.UserService;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
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
    private UsualMapper usualMapper;

    @Override
    public BaseResult<UserDto> loginProcess(HttpServletRequest request, HttpServletResponse response, String code) {
        BaseResult<TokenUser> userResult;
        if (YmlConfig.loginTest != null && "true".equals(YmlConfig.loginTest)) {
            //{"userId": "wangyongjun1","userNumber": "0000001295"}这里用userId
            String[] arr = code.split("!");
            TokenUser user = new TokenUser();
            user.userNumber = arr[0];
            user.userId = arr[1];
            userResult = BaseResult.success(user);
        } else {
            //统一认证获取token信息
            StringBuffer url = new StringBuffer(MobileAuthConfig.tokenUrl)
                    .append("?client_id=").append(MobileAuthConfig.clientId)
                    .append("&client_secret=").append(MobileAuthConfig.secret)
                    .append("&grant_type=authorization_code")
                    .append("&redirect_uri=").append(MobileAuthConfig.redirectUrl)
                    .append("&code=").append(code);
            log.info("移动OA统一认证...to get token url:{}", url.toString());
            HttpRequest post = HttpUtil.createPost(url.toString());
            HttpResponse tokenResponse = post.execute();
            log.info("移动OA统一认证...to get token result status:{}, body:{}", tokenResponse.getStatus(), tokenResponse.body());
            if (HttpStatus.HTTP_OK != tokenResponse.getStatus()) {
                log.error("移动OA统一认证...to get token response_body:{}", tokenResponse.body());
                return BaseResult.fail("OA统一认证getToken异常!");
            }
            String body = tokenResponse.body();
            ResponseToken responseToken = JSON.parseObject(body, ResponseToken.class);
            //移动OA统一认证token换取用户信息
            userResult = getUserByToken(responseToken.accessToken);
            if (!userResult.isSuccess()) {
                return BaseResult.fail(userResult.getMessage());
            }
        }
        //{"userId": "wangyongjun1","userNumber": "0000001295"}这里用userId
        TokenUser tUser = userResult.getData();
        String account = tUser.userId;
        //account登录KE,门户-请求页面设置cookie
        BaseResult<UserDto> loginResult = login(response, account);
        return loginResult;
    }

    private BaseResult<TokenUser> getUserByToken(String accessToken) {
        StringBuffer url = new StringBuffer(MobileAuthConfig.userInfoUrl).append("?accesstoken=").append(accessToken);
        log.info("移动OA统一认证-请求用户信息 accessToken:{}, url:{}", accessToken, url.toString());
        HttpRequest post = HttpUtil.createPost(url.toString());
        HttpResponse response = post.execute();
        log.info("移动OA统一认证-请求用户信息 result status:{}, body:{}", response.getStatus(), response.body());
        if (response.getStatus() == HttpStatus.HTTP_OK) {
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
    private BaseResult<UserDto> login(HttpServletResponse response, String account) {
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
        JSONObject kebody = JSON.parseObject(rbody);
        if (kebody.getInteger("error_code") != 200) {
            log.error("移动OA统一认证-登录KE 异常 body:{}", rbody);
            return BaseResult.fail("移动OA统一认证-登录KE 异常");
        }

        //登录门户
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

        //请求页面设置cookie
        setLoginCookie(response, userInfoData, account, kebody.getJSONObject("data").getString("session"));
        UserDto userDto = JSONObject.parseObject(userInfoData.toJSONString(), UserDto.class);
        userDto.setRedirectUrl(YmlConfig.oaHome);
        userDto.setToken(keToken);
        return BaseResult.success(userDto);
    }

    private void setLoginCookie(HttpServletResponse response, JSONObject userInfoData, String wsAccount, String session) {
        String account = userInfoData.getString("account");
        String path = "/";
        Cookie sidCookie = new Cookie(CookieKey.SESSION_ID, userInfoData.getString("sessionId"));
        sidCookie.setPath(path);
        response.addCookie(sidCookie);
        try {
            BASE64Encoder encoder = new BASE64Encoder();
            Cookie nameCookie = new Cookie(CookieKey.USER_NAME, encoder.encode(userInfoData.getString("name").getBytes("UTF-8")));
            nameCookie.setPath(path);
            response.addCookie(nameCookie);
            Cookie accountCookie = new Cookie(CookieKey.ACCOUNT, encoder.encode(account.getBytes("UTF-8")));
            accountCookie.setPath(path);
            response.addCookie(accountCookie);
            String bankName = AccountBankUtil.getAccountBankName(account, userInfoData.getString("sessionId"));
            List<JSONObject> bank = usualMapper.findBankByName(bankName);
            //如果未查到按总行处理
            String bankId = bank.size() == 0 ? "0" : bank.get(0).getString("bankId");
            log.info("account: {}，从流程引擎获取银行名称bankName: {}，从数据库查询银行bank: {}，最终的bankId: {}", account, bankName, bank.toString(), bankId);
            Cookie bankCookie = new Cookie(CookieKey.USER_BANK_ID, bankId);
            bankCookie.setPath(path);
            response.addCookie(bankCookie);
            Cookie authTokenCookie = new Cookie(CookieKey.AUTH_TOKEN, session);
            authTokenCookie.setPath(path);
            response.addCookie(authTokenCookie);
            Cookie wsCookie = new Cookie(CookieKey.WS_ACCOUNT, account);
            wsCookie.setPath(path);
            response.addCookie(wsCookie);
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
        //请求源-MOBILE_OA
        String srcEncode = AESCodeUtil.getSrcEncode(YmlConfig.keEncrtyKey, wsAccount, "MOBILE_OA");
        Cookie srcCookie = new Cookie(CookieKey.REQUEST_SOURCE, srcEncode);
        srcCookie.setPath(path);
        response.addCookie(srcCookie);
    }

    /**
     * 民生银行返回token信息
     */
    static class ResponseToken {
        public String accessToken;
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
