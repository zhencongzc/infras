package com.cmbc.infras.login.access.service.impl;

import cn.com.sense.oauth.client.OAuthClient;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.login.access.rpc.UserBankRpc;
import com.cmbc.infras.login.access.service.LoginService;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Service
public class LoginServiceImpl implements LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginServiceImpl.class);

    @Resource
    private UserBankRpc userBankRpc;
    @Resource
    private OAuthClient oAuthClient;


    @Override
    public String getKeToken(HttpServletRequest request, HttpServletResponse response, String code, String target) {
        String result = "";
        UserDto user = new UserDto();
        String atuhUrl;
        try {
            atuhUrl = YmlConfig.authorizeUrl + "?=client_id" +
                    YmlConfig.clientId + "&redirect_uri=" + URLEncoder.encode(YmlConfig.redirectUrl, "UTF-8");
            //默认跳转到行方登录验证页面
            user.setRedirectUrl(atuhUrl);
            if (YmlConfig.loginTest != null && "true".equals(YmlConfig.loginTest)) {
                //测试代码
                String[] arr = code.split("!");
                result = "{\"ERRORCODE\":\"0000\",\"ERRORMSG\":\"成功\",\"USERSN\":\"" + arr[0] + "\",\"CMBCOANAME\":\"" + arr[1] + "\",\"SN\":\"赵棣\",\"USERTYPE\":\"1000000000\",\"TELEPHONENUMBER\":\"010xxxxxxxx\",\"STATUS\":\"A\",\"SEX\":\"M\",\"MOBILE\":\"152xxxxxxxx\",\"MAIL\":\"xxxxxx@cmbc.com.cn\",\"JOBLIS\":[{\"POSITIONNAME\":\"泉州田安路支行零售银行部理财经理岗\",\"JOBID\":\"000188\",\"JOBNAME\":\"职员\",\"DEPTNAME\":\"泉州田安路支行零售银行部\",\"JOBORDER\":\"0\",\"DEPTID\":\"QUZ0000026\",\"POSITIONID\":\"QUZ00655\"},{\"POSITIONNAME\":\"泉州分行团委委员岗\",\"JOBID\":\"000188\",\"JOBNAME\":\"职员\",\"DEPTNAME\":\"北京管理部公司业务一处\",\"JOBORDER\":\"1\",\"DEPTID\":\"BEJ0000027\",\"POSITIONID\":\"QUZ00273\"}],\"BIRTHDAY\":\"19840502\",\"ACCOUNTID\":\"9123456789\"}";
            } else {
                String redirectUrl = YmlConfig.redirectUrl;
                if ("alarmPage".equals(target)) {   //跳转到三方告警页
                    //userDto.setRedirectUrl("/infras_web/alarm/event.html");
                    redirectUrl = redirectUrl.replace("/toLoginAccess", "/toAlarmPage");
                } else if ("lastCount".equals(target)) {    //访问三方查询数据接口
                    //userDto.setRedirectUrl("/infras/alarm/lastCount");
                    redirectUrl = redirectUrl.replace("/toLoginAccess", "/toLastCount");
                }
                logger.info("lg-LoginServiceImpl.callback getServerInfo redirect:{}", redirectUrl);
                result = oAuthClient.getServerInfo(request, response, URLEncoder.encode(redirectUrl, "UTF-8"));
            }
            logger.info("lg-LoginServiceImpl.callback getServerInfo--result:" + result);
        } catch (Exception e) {
            logger.error("lg-LoginServiceImpl.callback getServerInfo msg:{}", e.getMessage(), e);
            return "";
        }
        if (StringUtils.isNotBlank(result)) {
            JSONObject json = JSONObject.parseObject(result);
            String errCode = json.getString("ERRORCODE");
            if ("0000".equals(errCode)) {
                String usersn = json.getString("USERSN");
                String cmbcoaname = json.getString("CMBCOANAME");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("account", cmbcoaname);
                jsonObject.put("time", (System.currentTimeMillis() / 1000));
                jsonObject.put("url", "/loginaccess/toLoginAdmin?/rack/");
                KeEncryptUtil keEncryptUtil = new KeEncryptUtil(YmlConfig.keEncrtyKey);
                return keEncryptUtil.encrypt(jsonObject.toJSONString());
            } else {
                logger.error("LoginServiceImpl.callback ERRORCODE:{}, error!", errCode);
                return "";
            }
        }
        return "";
    }

    @Override
    public BaseResult<UserDto> callBack(HttpServletRequest request, HttpServletResponse response, String code, String target) {

        BaseResult<UserDto> loginResult = null;
        try {
            loginResult = loginKe(response, target);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (loginResult.isSuccess()) {
            logger.info("LoginServiceImpl.callback User.redirect:{}", loginResult.getData().getRedirectUrl());
        }
        return loginResult;

    }

    /**
     * 登录ke 操作
     * 1.登录ke  2.登录成功后拿到用户信息去登录门户
     *
     * @return 返回用户信息
     */
    private BaseResult<UserDto> loginKe(HttpServletResponse response, String target) throws Exception {

        JSONObject sessionObj = new JSONObject();
        sessionObj.put("session", UserContext.getAuthToken());
        sessionObj.put("id", UserContext.getCookieValue(CookieKey.USER_ID));
        sessionObj.put("account", UserContext.getCookieValue(CookieKey.ACCOUNT));
        sessionObj.put("name", UserContext.getCookieValue(CookieKey.USER_NAME));
        logger.info("sessionObj====" + sessionObj.toJSONString());

        logger.info("LoginServiceImpl.loginPortal url:{}", YmlConfig.simpleAdminUrl + "/auth/sso/login");
        HttpRequest post = HttpUtil.createPost(YmlConfig.simpleAdminUrl + "/auth/sso/login");
        post.body(sessionObj.toJSONString());
        String body = post.execute().body();
        logger.info("LoginServiceImpl.loginPortal 门户登录结果 result:{}", body);
        JSONObject simpleAdminLoginObj = JSONObject.parseObject(body);
        int code = simpleAdminLoginObj.getInteger("code");
        if (code != 200) {
            return BaseResult.fail(simpleAdminLoginObj.getString("error_msg"));
        }
        JSONObject userInfoData = simpleAdminLoginObj.getJSONObject("data").getJSONObject("user");
        setLoginCookie(response, userInfoData);
        UserDto userDto = JSONObject.parseObject(userInfoData.toJSONString(), UserDto.class);

        if ("alarmPage".equals(target)) {   //跳转到三方告警页
            userDto.setRedirectUrl(YmlConfig.redirectAlarmPage);
        } else if ("lastCount".equals(target)) {    //访问三方查询数据接口
            userDto.setRedirectUrl("/infras/alarm/lastCount");
        } else {
            //target=='home'及它--跳转到门户首页
            userDto.setRedirectUrl("/portal");
        }

        logger.info(userDto.getRedirectUrl());
        userDto.setToken(simpleAdminLoginObj.getJSONObject("data").getString("token"));
        return BaseResult.success(userDto);
    }

    /**
     * 设置cookie
     *
     * @param response     resp
     * @param userInfoData 用户信息
     */
    private void setLoginCookie(HttpServletResponse response, JSONObject userInfoData) {
        String path = "/";
        Cookie sidCookie = new Cookie(CookieKey.SESSION_ID, userInfoData.getString("sessionId"));
        sidCookie.setPath(path);
        response.addCookie(sidCookie);
        try {
            BASE64Encoder encoder = new BASE64Encoder();
            Cookie nameCookie = new Cookie(CookieKey.USER_NAME, encoder.encode(userInfoData.getString("name").getBytes("UTF-8")));
            nameCookie.setPath(path);
            response.addCookie(nameCookie);
            Cookie accountCookie = new Cookie(CookieKey.ACCOUNT, encoder.encode(userInfoData.getString("account").getBytes("UTF-8")));
            accountCookie.setPath(path);
            response.addCookie(accountCookie);
        } catch (Exception e) {
            logger.error("设置全局缓存出错", e);
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
        String wsAccount = userInfoData.getString("account");
        //请求源-PC_OA
        String srcEncode = AESCodeUtil.getSrcEncode(YmlConfig.keEncrtyKey, wsAccount, "PC_OA");
        Cookie srcCookie = new Cookie(CookieKey.REQUEST_SOURCE, srcEncode);
        srcCookie.setPath(path);
        response.addCookie(srcCookie);

    }

    /**
     * 移动OA回调处理
     */
    @Override
    public BaseResult<UserDto> mobileCallback(String code) {
        System.out.println("LoginServiceImpl.mobileCallback...code:" + code);
        StringBuffer url = new StringBuffer(MobileAuthConfig.tokenUrl)
                .append("?client_id=").append(MobileAuthConfig.clientId)
                .append("&client_secret=").append(MobileAuthConfig.secret)
                .append("&grant_type=authorization_code")
                .append("&redirect_uri=").append(MobileAuthConfig.redirectUrl)
                .append("&code=").append(code);
        logger.info("LoginServiceImpl.mobileCallback request url:{}", url);
        HttpRequest post = HttpUtil.createPost(url.toString());
        HttpResponse response = post.execute();
        logger.info("请求 get access_token status:{}, body:{}", response.getStatus(), response.body());
        if (response.getStatus() == HttpStatus.HTTP_OK) {
            String body = response.body();
            ResponseToken responseToken = JSON.parseObject(body, ResponseToken.class);
            TokenUser tokenUser = getUserByToken(responseToken.accessToken);
            System.out.println("请求用户信息:--info:" + JSON.toJSONString(tokenUser));
        }
        return null;
    }

    private TokenUser getUserByToken(String accessToken) {
        StringBuffer url = new StringBuffer(MobileAuthConfig.userInfoUrl).append("?accesstoken=").append(accessToken);
        logger.info("请求用户信息--LoginServiceImpl.getUserByToken---accessToken:{},url:{}", accessToken, url.toString());
        HttpRequest post = HttpUtil.createPost(url.toString());
        HttpResponse response = post.execute();
        logger.info("请求用户信息--LoginServiceImpl.getUserByToken 结果--status:{}, body:{}", response.getStatus(), response.body());
        if (response.getStatus() == HttpStatus.HTTP_OK) {
            String body = response.body();
            TokenUser user = JSON.parseObject(body, TokenUser.class);
            return user;
        }
        return null;
    }

    @Override
    public BaseResult<String> logout(String token) {
        String redisKey = InfrasConstant.AUTH_LOGIN + token;
        long l = DataRedisUtil.delete(redisKey);
        System.out.println("LoginServiceImpl.logout->Redis.delete:key" + redisKey + ",result long:" + l);
        StringBuffer url = new StringBuffer(YmlConfig.logoutUrl);
        try {
            url.append("?response_type=code")
                    .append("&client_id=").append(YmlConfig.clientId)
                    .append("&redirect_uri=").append(URLEncoder.encode(YmlConfig.redirectUrl, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.out.println("AuthorizeFilter.filter...exception");
            e.printStackTrace();
            String errmsg = e.getMessage();
            if (errmsg.length() > 500) {
                errmsg = errmsg.substring(0, 500);
            }
            return BaseResult.fail("AuthorizeFilter.filter...exception " + errmsg);
        }
        return BaseResult.success(url.toString());
    }

    /**
     * 民生银行返回token信息
     */
    private class ResponseToken {
        public int expiresIn;
        public String accessToken;
        public String refreshToken;
    }

    /**
     * 通过accessToken
     * 查询User信息
     */
    private class TokenUser {
        public String userId;
        public String userNumber;
    }

}
