package com.cmbc.infras.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import javax.servlet.http.HttpServletResponse;

/**
 * @description: <p> cookie保存的key </p>
 * @author: LongQi
 * @date: 2020/12/1 14:19
 */
public class CookieKey {

    /**
     * 用户账号
     */
    public static final String ACCOUNT = "ACCOUNT";
    /**
     * 用户姓名
     */
    public static final String USER_NAME = "USER_NAME";
    /**
     * 用户ID
     */
    public static final String USER_ID = "USER_ID";
    /**
     * 用户组织ID
     */
    public static final String ORG_ID = "ORG_ID";
    /**
     * 用户部门ID
     */
    public static final String DEPT_ID = "DEPT_ID";
    /**
     * session id
     */
    public static final String SESSION_ID = "X_GU_SID";
    /**
     * 主题
     */
    public static final String THEME = "THEME";
    /**
     * 模式
     */
    public static final String MODE = "MODE";
    /**
     * 语言
     */
    public static final String LOCAL_LANG = "LOCAL_LANG";
    /**
     * 项目名
     */
    public static final String X_PRODUCT = "X_PRODUCT";
    /**
     * 用户数据
     */
    public static final String USER_INFO = "USER_INFO";
    /**
     * 集中监控加bankId
     */
    public static final String USER_BANK_ID = "BANK_ID";
    /** 统一认证 code */
    /**
     * 20211110-值成登录KE时返回的token
     */
    public static final String AUTH_TOKEN = "CODE";
    /**
     * WebSocket推送需要account-加WS前缀避免冲突
     */
    public static final String WS_ACCOUNT = "WS_ACCOUNT";
    /**
     * 请求方
     * 1:PC端,2:移动OA
     */
    public static final String REQUEST_SOURCE = "REQ_SRC";
}
