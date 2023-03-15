package com.cmbc.infras.system.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        //检查url参数
        String queryString = ((HttpServletRequest) request).getQueryString();
        if (isSpecialChar(queryString)) {
            response.setCharacterEncoding("utf-8");
            String json = "很抱歉，由于您访问的URL有可能对网站造成安全威胁，您的访问被阻断。";
            resp.getWriter().write(json);
            return;
        }
        //检查body数据
        MyHttpServletRequestWrapper req = new MyHttpServletRequestWrapper((HttpServletRequest) request);
        String bodyString = req.getBodyString();
        if (!StringUtils.isBlank(bodyString)) {
            Map<String, Object> params = JSON.parseObject(bodyString, Map.class);
            Set<String> keySet = params.keySet();
            for (String param : keySet) {
                if (isSpecialChar(String.valueOf(params.get(param)))) {
                    response.setCharacterEncoding("utf-8");
                    String json = "很抱歉，由于您访问的URL有可能对网站造成安全威胁，您的访问被阻断。";
                    resp.getWriter().write(json);
                    return;
                }
            }
        }
        chain.doFilter(req, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    public static boolean isSpecialChar(String str) {
        if (str == null) {
            return false;
        }
        String regEx[] = new String[]{"<script>(.*?)</script>", "<((?i)script)[^>]?>[\\s\\S]?<\\/((?i)script)>", "src[\\r\\n]*=[\\r\\n]*\\\\'(.*?)\\\\'",
                "<script(.*?)>", "</script>", "eval\\((.*?)\\)", "expression\\((.*?)\\)", "javascript:", "vbscript:", "onload(.*?)="
                , "<((?i)style)[^>]?>[\\s\\S]?<\\/((?i)style)>"};
        for (String reg : regEx) {
            boolean b = Pattern.compile(reg).matcher(str.toLowerCase()).find();
            if (b) return b;
        }
        return false;
    }

}
