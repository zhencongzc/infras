package com.cmbc.infras.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 移动OA端统一登录认证配置项
 */
@Component
public class MobileAuthConfig {

    public static String clientId;//clientId
    public static String secret;//secret
    public static String redirectUrl;//重定向URL
    public static String loginUrl;//登录URL-行方
    public static String loginKeUrl;//登录后台URL-loginaccess登录后台
    public static String authUrl;//跳转统一登录认证URL
    public static String tokenUrl;//用code取token URL
    public static String userInfoUrl;//token取user URL
    public static String mobileUrls;//移动OA端允许记问的url
    public static List<String> mobileUrlList;

    @Value("${mobile.auth.clientId:}")
    public void setClientId(String clientIdVal) {
        clientId = clientIdVal;
    }

    @Value("${mobile.auth.secret:}")
    public void setSecret(String secretVal) {
        secret = secretVal;
    }

    @Value("${mobile.auth.redirectUrl:}")
    public void setRedirectUrl(String redirect) {
        redirectUrl = redirect;
    }

    @Value("${mobile.auth.loginUrl:}")
    public void setLoginUrl(String login) {
        loginUrl = login;
    }

    @Value("${mobile.auth.loginKeUrl:}")
    public void setLoginKeUrl(String login) {
        loginKeUrl = login;
    }

    @Value("${mobile.auth.authUrl:}")
    public void setAuthUrl(String auth) {
        authUrl = auth;
    }

    @Value("${mobile.auth.tokenUrl:}")
    public void setTokenUrl(String token) {
        tokenUrl = token;
    }

    @Value("${mobile.auth.userInfoUrl:}")
    public void setUserInfoUrl(String userInfo) {
        userInfoUrl = userInfo;
    }

    @Value("${mobile.auth.mobileUrls:}")
    public void setMobileUrls(String value) {
        mobileUrls = StringUtil.removeSpecialChar(value);
        mobileUrlList = new ArrayList<>();
        String[] arr = mobileUrls.split(",");
        for (int i = 0; i < arr.length; i++) {
            String item = arr[i].trim();
            mobileUrlList.add(item);
        }
    }

}
