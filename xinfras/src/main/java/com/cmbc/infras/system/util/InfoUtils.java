package com.cmbc.infras.system.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class InfoUtils {

    /**
     * 获取Header信息
     *
     * @param request
     * @return
     */
    public static String getHeadersInfo(HttpServletRequest request) {
        StringBuffer sb = new StringBuffer();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            sb.append(key).append("=").append(value).append(",");
        }
        return sb.toString();
    }

    /**
     * 获取Cookie信息
     *
     * @param request
     * @return
     */
    public static String getCookieInfo(HttpServletRequest request) {
        StringBuffer sb = new StringBuffer();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String key = cookie.getName();
                String value = cookie.getValue();
                sb.append(key).append("=").append(value).append(",");
            }
        }
        return sb.toString();
    }

    public static String byteArrToHex(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

}
