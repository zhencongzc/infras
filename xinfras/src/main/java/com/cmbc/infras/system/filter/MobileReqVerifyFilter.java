package com.cmbc.infras.system.filter;

import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.util.AESCodeUtil;
import com.cmbc.infras.util.CookieKey;
import com.cmbc.infras.util.MobileAuthConfig;
import com.cmbc.infras.util.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Decoder;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
public class MobileReqVerifyFilter implements Filter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        String cookieStr = this.getCookie(req, CookieKey.REQUEST_SOURCE);
        if (StringUtils.isEmpty(cookieStr)) {
            log.info("没有取到cookie[{}]的信息,门户登录,可全路径访问", CookieKey.REQUEST_SOURCE);
            chain.doFilter(request, response);
            return;
        }
        String account = null;
        String acc = getCookie(req, CookieKey.ACCOUNT);
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            byte[] bytes = decoder.decodeBuffer(acc);
            account = new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String code = AESCodeUtil.getSrcDecode(YmlConfig.keEncrtyKey, cookieStr);
        JSONObject obj = JSONObject.parseObject(code);
        String deSrc = obj.getString(CookieKey.REQUEST_SOURCE);
        //请求源为PC端无限制路径,放行
        if (deSrc.equals("PC_OA")) {
            chain.doFilter(request, response);
            return;
        } else if (deSrc.equals("MOBILE_OA")) {//移动OA,校验请求路径
            boolean b = checkMobileAccess(path);
            if (b) {
                chain.doFilter(request, response);
                return;
            } else {
                log.error("移动端无权访问url:{}", path);
                return;
            }
        } else {
            log.error("请求源解析异常,SRC:{}", deSrc);
            return;
        }
    }


    private boolean checkMobileAccess(String path) {
        List<String> list = MobileAuthConfig.mobileUrlList;
        //未设置移动白名单,全部放行
        if (list.size() == 0) {
            return true;
        }
        //如果设置了白名单,则匹配的才可以放行(true)
        for (String url : list) {
            if (path.contains(url)) {
                return true;
            }
        }
        return false;
    }

    private String getCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
//        //移动端专用cookie
//        String mobileCookie = req.getHeader("h5-cookie");
//        HashMap<String, String> cookieMap = new HashMap<>();
//        if (StringUtils.isNotBlank(mobileCookie)) {
//            log.info("===========mobileCookie:{}", mobileCookie);
//            String[] split = mobileCookie.split(";");
//            for (String s : split) {
//                String trim = s.trim();
//                String key = trim.substring(0, trim.indexOf("="));
//                String value = trim.substring(trim.indexOf("=") + 1);
//                cookieMap.put(key, value);
//            }
//        }
        //cookie校验
        if (cookies == null || cookies.length == 0) {
//            String s = cookieMap.get(name);
//            log.info("===========cookieMap.get({}):{}", name, s);
            log.info("未取到cookie!");
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                String value = cookie.getValue();
                log.info("已取到cookieName:{},value:{}", cookie.getName(), value);
                return value == null ? "" : value;
            }
        }
        return "";
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}
