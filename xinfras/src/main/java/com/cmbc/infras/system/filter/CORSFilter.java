package com.cmbc.infras.system.filter;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@Order(-10)
public class CORSFilter implements Filter {

    private FilterConfig config;

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) resp;
        HttpServletRequest request = (HttpServletRequest) req;
        //response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Origin, Accept, x-requested-with, authorization, Content-Type, Authorization, credential, X-XSRF-TOKEN,XFILENAME,XFILECATEGORY,XFILESIZE");
        response.setHeader("Content-Type", "application/json;charset=utf-8");

        //解决 Content-Type:application/json 时跨域设置失败问题
        /**
         * "Access-Control-Allow-Origin" 不能设置成 "*"
         * 当 Content-Type 为 application/json 时，Ajax实际会发两次请求，第一次是一个OPTIONS请求，这时过滤器一定得放开
         */
        /*if("OPTIONS".equals(request.getMethod())) {
            //放行OPTIONS请求
            System.out.println("dofilter.....req:" + request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_OK);
        }*/

        //chain.doFilter(request, response);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, resp);
        }

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = filterConfig;
    }
}
