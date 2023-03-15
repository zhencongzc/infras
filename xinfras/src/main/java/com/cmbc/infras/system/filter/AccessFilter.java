package com.cmbc.infras.system.filter;

import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.auth.UserDto;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.DataConfigMapper;
import com.cmbc.infras.util.AccountBankUtil;
import com.cmbc.infras.util.CookieKey;
import com.cmbc.infras.util.UserContext;
import com.cmbc.infras.util.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
@Component
public class AccessFilter implements Filter {


    @Resource
    private DataConfigMapper dataConfigMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        //通过Header配置的白名单
        if ("TRUE".equals(req.getHeader("WHITE_LIST"))) {
            chain.doFilter(request, response);
            return;
        }
        //通过配置文件配置的白名单
        String path = req.getRequestURI();
        log.info("AccessFilter start...  request uri = " + path);
        for (String s : YmlConfig.whiteList) {
            if (path.contains(s)) {
                log.info("request path in whiteList. Pass path:" + path + "  string=" + s);
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
        //从 header,parameter中取code，如果都为空则是未登录访问,跳转登行方登录
        String token = UserContext.getAuthToken();
        log.info("AccessFilter.doFilter --sys...UserContext.getAuthCode()----code:" + token + ",path:" + path);
        if (StringUtils.isBlank(token)) {
            toOauth(req, res, redirectUrl, chain);
            return;
        }
        //如果redis不存在key或超时,也跳转行方统一认证
        String redisKey = InfrasConstant.AUTH_LOGIN + token;
        UserDto user = DataRedisUtil.getStringFromRedis(redisKey, UserDto.class);
        if (user == null) {
            toOauth(req, res, redirectUrl, chain);
            return;
        } else {
            String bankId = UserContext.getUserBankId();
            String spath = "/";
            String account = UserContext.getUserAccount();
            String sessionId = user.getSessionId();
            //从流程引擎获取银行名称
            String bankName = AccountBankUtil.getAccountBankName(account, sessionId);
            if ("机房管理中心".equals(bankName)) bankName = "中国民生银行";
            //从数据库查询银行id
            List<JSONObject> bank = dataConfigMapper.findBankByName(bankName);
            if (bank.size() != 0) {
                String bankIdNow = bank.get(0).getString("bankId");
                if (!bankIdNow.equals(bankId)) {
                    Cookie bankCookie = new Cookie(CookieKey.USER_BANK_ID, bankIdNow);
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
        }
        chain.doFilter(request, response);
    }

    /**
     * 跳转oauth统一认证
     * 如果是移动端,则跳转移动端统一认证
     * 12.2改版-移动端统一认证真接回调-http://197.0.3.78:18080/infras/app/callback
     */
    private void toOauth(HttpServletRequest req, HttpServletResponse res, String redirectUrl, FilterChain chain) throws IOException {
        StringBuffer url = new StringBuffer(YmlConfig.authorizeUrl)
                .append("?response_type=code")
                .append("&client_id=").append(YmlConfig.clientId)
                .append("&redirect_uri=").append(URLEncoder.encode(redirectUrl, "UTF-8"));
        log.info("未登录状态:跳转url:" + url.toString());
        res.setStatus(HttpStatus.HTTP_MOVED_TEMP);
        res.setHeader("Location", url.toString());
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}

