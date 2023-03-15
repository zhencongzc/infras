package com.cmbc.infras.util;

import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.redis.DataRedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sun.misc.BASE64Decoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
public class UserContext {

    public static String getUserBankId() {
        String value = getCookieValue(CookieKey.USER_BANK_ID);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }

        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        Object idObj = request.getAttribute(CookieKey.USER_BANK_ID);
        if (idObj instanceof String) {
            return (String) idObj;
        }
        return null;
    }

    public static String getUserAccount() {
        BASE64Decoder decoder = new BASE64Decoder();
        String enacc = getCookieValue(CookieKey.ACCOUNT);
        String account = "";
        try {
            byte[] bytes = decoder.decodeBuffer(enacc);
            account = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return enacc;
        }
        return account;
    }

    public static String getCookieValue(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static UserDto getCurrentUser() {
        String token = getAuthToken();
        String redisKey = InfrasConstant.AUTH_LOGIN + token;
        UserDto user = DataRedisUtil.getStringFromRedis(redisKey, UserDto.class);
        if (user != null) {
            return user;
        }
        return null;
    }

    /**
     * LoginController#authorizeCallback
     * ->LoginServiceImpl#callBack
     * 中设置的cookie
     */
    public static String getAuthToken() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CookieKey.SESSION_ID.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * getRequest
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

}
