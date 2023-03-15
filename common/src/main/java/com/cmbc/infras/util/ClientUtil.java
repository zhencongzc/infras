package com.cmbc.infras.util;

import cn.hutool.extra.servlet.ServletUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @desc 客户端操作
 */
public class ClientUtil {

    /**
     * 获取客户端IP
     * @return 客户端IP
     */
    public static String getClientIP(){
        HttpServletRequest request = getRequest();
        if (request == null){
            return StringUtils.EMPTY;
        }
        return ServletUtil.getClientIP(request);
    }

    /**
     * 获取请求的路径，不包括IP
     * @return 请求路径
     */
    public static String getRequestUri(){
        HttpServletRequest request = getRequest();
        if (request == null){
            return StringUtils.EMPTY;
        }
        return request.getRequestURI();
    }



    /**
     * 获取全局的request
     * @return request对象
     */
    public static HttpServletRequest getRequest(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null){
            return null;
        }
        return attributes.getRequest();
    }
}
