package com.cmbc.infras.health.util;


public class CommonUtils {

    /**
     * 转换周期单位(秒)
     */
    public static long change(String cycleUnit) {
        switch (cycleUnit) {
            case "分钟":
                return 60;
            case "小时":
                return 60 * 60;
            case "天":
                return 60 * 60 * 24;
            case "周":
                return 60 * 60 * 24 * 7;
            case "月":
                return 60 * 60 * 24 * 30;
            case "季度":
                return 60 * 60 * 24 * 90;
            case "年":
                return 60 * 60 * 365;
        }
        return 0;
    }

}
