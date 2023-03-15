package com.cmbc.infras.util;

import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtils {

    public static DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    /**
     * 取得当月0点时间
     */
    public static long getCurrentMonthDot() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date d = calendar.getTime();
        return calendar.getTimeInMillis() / 1000;
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 获取当天零点时间整形值
     */
    public static long getTodayZeroDot() {
        long current = System.currentTimeMillis();//当前时间毫秒数
        long zero = current / (1000 * 3600 * 24) * (1000 * 3600 * 24) - TimeZone.getDefault().getRawOffset();//今天零点零分零秒的毫秒数
        return zero / 1000;
    }

    /**
     * 获取当前时间整形值
     */
    public static String getCurrentTime(String format) {
        DateFormat df = null;
        if (StringUtils.isEmpty(format)) {
            df = new SimpleDateFormat(DATE_FORMAT);
        } else {
            df = new SimpleDateFormat(format);
        }
        return df.format(new Date());
    }

    public static String transToStr(int time) {
        if (time == 0) {
            return "-";
        }
        //KE平台时间统一加3位0
        Long l = Long.parseLong(time + "000");
        return df.format(new Date(l));
    }

    /**
     * 取得当日0点时间字符串
     */
    public static String getTodayZeroFormat() {
        return getTodayZeroFormat(DATE_FORMAT);
    }

    public static String getTodayZeroFormat(String format) {
        DateFormat df = new SimpleDateFormat(format);
        long current = System.currentTimeMillis();//当前时间毫秒数
        long zero = current / (1000 * 3600 * 24) * (1000 * 3600 * 24) - TimeZone.getDefault().getRawOffset();//今天零点零分零秒的毫秒数
        String ds = df.format(zero);
        return ds;
    }

    /**
     * 取得当前时间字符串
     */
    public static String getCurrentFormat() {
        return getCurrentFormat(DATE_FORMAT);
    }

    public static String getCurrentFormat(String format) {
        DateFormat df = new SimpleDateFormat(format);
        String ds = df.format(new Date());
        return ds;
    }

    public static void main(String[] args) {
        //System.out.println("当月时间:" + DateTimeUtils.getCurrentMonthDot());
        //System.out.println("当前时间:" + System.currentTimeMillis()/1000);
    }

}
