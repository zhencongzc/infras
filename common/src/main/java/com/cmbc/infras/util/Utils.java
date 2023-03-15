package com.cmbc.infras.util;

import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {

    public static DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";


    public static <T> List<T> removeDuplicate(List<T> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        TreeSet<T> set = new TreeSet<>(list);
        list.clear();
        list.addAll(set);
        return list;
    }

    public static String transToStr(int time) {
        if (time == 0) {
            return "-";
        }
        //KE平台时间统一加3位0
        Long l = Long.parseLong(time + "000");
        return df.format(new Date(l));
    }

    public static long getTodayZeroDot() {
        long current=System.currentTimeMillis();//当前时间毫秒数
        long zero=current/(1000*3600*24)*(1000*3600*24) - TimeZone.getDefault().getRawOffset();//今天零点零分零秒的毫秒数
        return zero;
    }

    public static String getCurrentTime(String format) {
        DateFormat df = null;
        if (StringUtils.isEmpty(format)) {
            df = new SimpleDateFormat(DATE_FORMAT);
        } else {
            df = new SimpleDateFormat(format);
        }
        return df.format(new Date());
    }

}
