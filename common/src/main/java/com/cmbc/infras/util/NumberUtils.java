package com.cmbc.infras.util;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

public class NumberUtils {

    public static float parseFloat(String s) {
        if (!isNumeric(s)) {
            System.out.println("NumberUtils.parseFloat input is not number:" + s);
            return 0f;
        }
        if (StringUtils.isBlank(s)) {
            return 0f;
        }
        float v = 0;
        try {
            v = Float.parseFloat(s);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0.0f;
        }
        return v;
    }

    /**
     * 计算百分比
     */
    public static String getPersent(int x, int sum) {
        DecimalFormat df = new DecimalFormat("0.0%");
        if (sum == 0) return "100%";
        if (x == 0) return "0%";
        double dx = x * 1.0;
        double dsum = sum * 1.0;
        double rd = dx / dsum;
        String result = df.format(rd);
        return result;
    }

    public static double fomatFloat(float f, int i) {
        return Math.round(f * Math.pow(10, i)) / Math.pow(10, i);
    }

    public static boolean isNumeric(String str) {
        if (null == str || "".equals(str)) {
            return false;
        }
        Pattern pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");
        return pattern.matcher(str).matches();
    }

    public static void main(String[] args) {
        String a = "-";
        System.out.println(isNumeric(a));
    }
}
