package com.cmbc.infras.system.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.util.YmlConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 * 业务代码工具类
 */
@Slf4j
public class BusinessUtil {

    /**
     * 告警匹配银行id
     * 判断HashSet中是否包含eventLocation字段第三个值，即告警位置
     */
    public static boolean alarmMatchBankId(AlarmEvent alarm, HashSet<String> set) {
        //截取告警位置，如果没有设置为"-1"
        String location = alarm.getEventLocation(); //project_root/0_931/0_969/0_1083/0_1183
        String[] arr = location.split("/");
        String rid = arr.length > 2 ? arr[2] : "-1";
        //匹配
        if (set.contains(rid)) return true;
        return false;
    }

    /**
     * 重载方法
     */
    public static boolean alarmMatchBankId(AlarmEvent alarm, List<String> bankIds) {
        //转化为哈希表
        HashSet<String> set = new HashSet<>();
        for (String bankId : bankIds) {
            set.add(bankId);
        }
        return BusinessUtil.alarmMatchBankId(alarm, set);
    }

    /**
     * 生成请求参数：移动OA代办消息推送
     * message：消息内容
     * touser：发送的用户
     */
    public static Object createParamForSendMessage(String message, List<String> touser) {
        //构造参数
        Map<String, Object> map = new HashMap<>();
        map.put("appId", "1125");//应用编号，行方开通的唯一编号
        map.put("msgtype", "pending");//消息类型，text:文本消息，pending:待办类消息
        map.put("touser", touser);//推送的用户
        //渠道配置
        Map<String, Object> impushChannel = new HashMap<>();//即时通消息渠道
        impushChannel.put("impushImportant", "yes"); //强制发送 yes:强制发送；no:正常
        impushChannel.put("impushContent", message);//即时通消息内容
        impushChannel.put("url", "测试跳转地址url");//待办消息跳转到具体页面地址
        map.put("impushChannel", impushChannel);
        //推送
        String mapStr = JSONObject.toJSONString(map);
        Object requestJson = JSON.parse(mapStr);
        return requestJson;
    }

    /**
     * 生成请求参数：移动OA邮件发送
     * message：消息内容
     * touser：发送的用户
     */
    @RequestMapping("/sendEmail")
    public static Object createParamForSendEmail(String message, List<String> touser) {
        //构造参数
        log.info("移动OA发送邮件开始...");
        Map<String, Object> map = new HashMap<>();
        map.put("appId", "1125");//应用编号，行方开通的唯一编号
        map.put("subject", "分行基础设施集中监控系统-代办通知");//邮件标题
        map.put("touser", touser);
        //渠道配置
        Map<String, Object> emailMap = new HashMap<>();//邮件渠道
        emailMap.put("emailImportant", "yes");//强制发送 yes:强制发送；no:正常
        emailMap.put("emailContent", message);//邮件内容
        map.put("emailChannel", emailMap);
        //发送
        String mapStr = JSONObject.toJSONString(map);
        Object requestJson = JSON.parse(mapStr);
        return requestJson;
    }

    /**
     * 生成请求参数：i民生飞书机器人消息
     * message：消息内容
     * touser：发送的用户
     */
    @RequestMapping("/sendFeishu")
    public static Object createParamForFeishu(String message, List<String> touser) {
        //构造参数
        log.info("飞书发送代办消息开始...");
        Map<String, Object> map = new HashMap<>();
        map.put("appId", "1125");//应用编号，行方开通的唯一编号
        map.put("touser", touser);
        //渠道配置
        Map<String, Object> feishuMap = new HashMap<>();//飞书渠道
        feishuMap.put("urgent", true);//是否紧急
        feishuMap.put("msgtype", "text");//消息类型
        JSONObject content = new JSONObject();
        content.put("text", message);
        feishuMap.put("content", content.toJSONString());//消息内容
        System.out.println(content.toJSONString());
        feishuMap.put("important", true);//强制发送
        feishuMap.put("emailImportant", true);//强制发送 yes:强制发送；no:正常
        feishuMap.put("callbackUrl", "飞书机器人回调接口");//回调接口
        map.put("feishuChannel", feishuMap);
        //发送
        String mapStr = JSONObject.toJSONString(map);
        Object requestJson = JSON.parse(mapStr);
        return requestJson;
    }

    /**
     * 移动OA代办消息通知，服务认证获取token
     */
    public static String getToken() {
        String url = YmlConfig.mobile_authorize_url;
        HttpRequest post = HttpUtil.createPost(url);
        post.form("client_id", YmlConfig.client_id);
        post.form("client_secret", YmlConfig.client_secret);
        post.form("username", YmlConfig.username);
        post.form("password", YmlConfig.password);
        post.form("grant_type", YmlConfig.grant_type);
        String body = post.execute().body();
        log.info("请求接口" + url + ",入参body: {},返回参数body: {}", post.form(), body);
        JSONObject res = JSONObject.parseObject(body);
        String access_token = res.getString("access_token");
        return access_token;
    }
}
