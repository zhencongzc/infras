package com.cmbc.infras.util;

import javax.servlet.http.HttpServletRequest;

public class DeviceUtils {

    public static boolean isMobileDevice(HttpServletRequest request) {
        String via = request.getHeader("Via");
        String userAgent = request.getHeader("user-agent");
        return isMobileDevice(via, userAgent);
    }

    public static boolean isMobileDevice(String via, String userAgent) {

        boolean isMobile = false;
        boolean pcFlag = false;
        boolean mobileFlag = false;

        for (int i = 0; via != null && !via.trim().equals("") && i < mobileGatewayHeaders.length; i++) {
            if (via.contains(mobileGatewayHeaders[i])) {
                mobileFlag = true;
                break;
            }
        }

        for (int i = 0; !mobileFlag && userAgent != null && !userAgent.trim().equals("") && i < mobileUserAgents.length; i++) {
            if (userAgent.contains(mobileUserAgents[i])) {
                mobileFlag = true;
                break;
            }
        }

        for (int i = 0; userAgent != null && !userAgent.trim().equals("") && i < pcHeaders.length; i++) {
            if (userAgent.contains(pcHeaders[i])) {
                pcFlag = true;
                break;
            }
        }

        if (mobileFlag == true && pcFlag == false) {
            isMobile = true;
        }

        return isMobile;
    }



    private static String[] mobileGatewayHeaders = new String[]{
            "ZXWAP",
            "chinamobile.com",
            "monternet.com",
            "infoX",
            "XMS 724Solutions HTG",
            "Bytemobile"
    };

    private static String[] pcHeaders = new String[]{
            "Windows 98",
            "Windows ME",
            "Windows 2000",
            "Windows XP",
            "Windows NT",
            "Ubuntu",
    };

    private static String[] mobileUserAgents = new String[]{
            "Nokia",
            "SAMSUNG",
            "MIDP-2",
            "CLDC1.1",
            "SymbianOS",
            "MAUI",
            "UNTRUSTED/1.0",
            "Window CE",
            "iPhone",
            "iPad",
            "Android",
            "BlackBerry",
            "UCWEB",
            "ucweb",
            "BREW",
            "J2ME",
            "YULONG",
            "YuLong",
            "COOLPAD",
            "TIANYU",
            "TY-",
            "K-Touch",
            "Hair",
            "DOPOD",
            "Lenovo",
            "LENOVE",
            "HUAQIN",
            "AIGO-",
            "CTC/1.0",
            "CTC/2.0",
            "CMCC",
            "DAXIAN",
            "MOT-",
            "SonyEricsson",
            "GIONEE",
            "HTC",
            "ZTE",
            "HUAWEI",
            "webOS",
            "GoBrowser",
            "IEMobile",
            "WAP2.0"
    };

}
