package com.cmbc.infras.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    public static String removeSpecialChar(String str){
        String s = "";
        if(str != null){
            // 定义含特殊字符的正则表达式
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            s = m.replaceAll("");
        }
        return s;
    }

    public static void main(String[] args) {
        String s = ".js,.png,.ttf,.woff,.css,.json,.webm,/logout,/authorizeCallback,/lastCount,/toLoginAccess,/mobileAuthCallback,/logout\n" +
                "    ,/socket,/alarm/authCode,/userBankId,/toAlarmPage,/toLastCount,/event.html,/app/callback,/loginProcess";
        System.out.println("before:" + s);
        s = removeSpecialChar(s);
        System.out.println("after:" + s);
    }

}
