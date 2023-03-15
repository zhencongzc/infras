package com.cmbc.infras.login.access.filter;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.login.access.mapper.UsualMapper;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

@Component
public class AccessFilter implements Filter {

    @Resource
    private UsualMapper usualMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();
        //白名单校验
        for (String s : YmlConfig.whiteList) {
            if (path.contains(s)) {
                System.out.println("request path in whiteList. Pass:  path=" + path + "  string=" + s);
                chain.doFilter(request, response);
                return;
            }
        }
        String redirectUrl = YmlConfig.redirectUrl;
        if (path.contains("/alarm/lastCount")) {
            redirectUrl = redirectUrl.replace("/toLoginAccess", "/toLastCount");
        } else if (path.contains("/alarm/")) {
            redirectUrl = redirectUrl.replace("/toLoginAccess", "/toAlarmPage");
        }
        //从 header,parameter中取code,如果都为空则是未登录访问,跳转登行方登录
        String token = UserContext.getAuthToken();
        System.out.println("UserContext.getAuthCode()-access----code:" + token);
        if (StringUtils.isBlank(token)) {
            System.out.println("code blank 跳转path:" + path + ",redirect:" + redirectUrl);
            toOauth(req, res, redirectUrl);
            return;
        }
        //如果redis不存在key或超时,也跳转行方统一认证
        String redisKey = InfrasConstant.AUTH_LOGIN + token;
        UserDto user = DataRedisUtil.getStringFromRedis(redisKey, UserDto.class);
        if (user == null) {
            System.out.println("user null 跳转path:" + path + ",redirect:" + redirectUrl);
            toOauth(req, res, redirectUrl);
            return;
        } else {
            String bankId = UserContext.getUserBankId();
            if (StringUtils.isBlank(bankId)) {
                String spath = "/";
                String account = UserContext.getUserAccount();
                String sessionId = user.getSessionId();
                //从流程引擎获取银行名称
                String bankName = AccountBankUtil.getAccountBankName(account, sessionId);
                //从数据库查询银行id
                List<JSONObject> bank = usualMapper.findBankByName(bankName);
                bankId = bank.get(0).getString("bankId");
                Cookie bankCookie = new Cookie(CookieKey.USER_BANK_ID, bankId);
                bankCookie.setPath(spath);
                res.addCookie(bankCookie);
                Cookie authTokenCookie = new Cookie(CookieKey.AUTH_TOKEN, token);
                authTokenCookie.setPath(spath);
                res.addCookie(authTokenCookie);
                Cookie wsCookie = new Cookie(CookieKey.WS_ACCOUNT, account);
                wsCookie.setPath(spath);
                res.addCookie(wsCookie);
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 跳转oauth统一认证
     * 如果是移动端,则跳转移动端统一认证
     */
    private void toOauth(HttpServletRequest req, HttpServletResponse res, String redirectUrl) throws UnsupportedEncodingException {
        //如果来源是APP,则跳转称动端统一认证
        String source = req.getHeader(InfrasConstant.APP_REQUEST_HEADER);
        if (StringUtils.isNotBlank(source) && InfrasConstant.APP_SOURCE.equals(source)) {
            Cookie srcCookie = new Cookie(InfrasConstant.APP_REQUEST_HEADER, InfrasConstant.APP_SOURCE);
            res.addCookie(srcCookie);
            toAppOauth(res);
            return;
        }

        StringBuffer url = new StringBuffer(YmlConfig.authorizeUrl)
                .append("?response_type=code")
                .append("&client_id=").append(YmlConfig.clientId)
                .append("&redirect_uri=").append(URLEncoder.encode(redirectUrl, "UTF-8"));
        System.out.println("未登录状态:跳转url:" + url.toString());
        //res.setStatus(HttpStatus.SEE_OTHER.value());
        res.setStatus(HttpStatus.FOUND.value());
        res.setHeader("Location", url.toString());
    }

    /**
     * 移动OA跳转-移动端统一认证
     */
    private void toAppOauth(HttpServletResponse res) {
        StringBuffer url = new StringBuffer(MobileAuthConfig.authUrl)
                .append("?response_type=code")
                .append("&client_id=").append(MobileAuthConfig.clientId)
                .append("&redirect_uri=").append(MobileAuthConfig.redirectUrl);
        System.out.println("未登录状态:跳转url:" + url.toString());
        //res.setStatus(HttpStatus.SEE_OTHER.value());
        res.setStatus(HttpStatus.FOUND.value());
        res.setHeader("Location", url.toString());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        /////
    }

    @Override
    public void destroy() {
        ////
    }
}
