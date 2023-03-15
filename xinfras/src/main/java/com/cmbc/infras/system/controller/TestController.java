package com.cmbc.infras.system.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.rpc.MobileOARpc;
import com.cmbc.infras.system.service.UserService;
import com.cmbc.infras.system.util.BusinessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;

/**
 * 用于测试第三方接口
 */
@RestController
@Slf4j
@RequestMapping("/test")
public class TestController {

    @Resource
    private UserService userService;
    @Resource
    private MobileOARpc mobileOARpc;
    @Resource
    private EventRpc eventRpc;

    /**
     * 测试OA消息推送
     */
    @RequestMapping("/sendMessage")
    public BaseResult<JSONObject> test1(String userName, String msgtype, String message) {
        log.info("移动OA推送消息开始...");
        //构造参数
        Map<String, Object> map = new HashMap<>();
        map.put("appId", "1125");//应用编号，行方开通的唯一编号
        map.put("msgtype", msgtype);//消息类型，text:文本消息，pending:待办类消息

        //推送的用户
        List<String> touser = new ArrayList<>();//用户名，qiaohesong,wangyongjun1
        touser.add(userName);
        map.put("touser", touser);
        //查询告警所属分行，查询KE角色：xx分行-基础设施管理员、xx分行-中心机房管理员、分行机房管理员，分别添加对应人员
        String bankName = "北京分行";
        Set<String> setRoles = new HashSet<>();
        setRoles.add(bankName + "-基础设施管理员");
        setRoles.add(bankName + "-中心机房值班员");
        setRoles.add("分行机房管理员");
        List<Integer> roleId = new LinkedList<>();//存放角色的id
        String str1 = eventRpc.getRoleList(InfrasConstant.KE_RPC_COOKIE);
        JSONObject json = JSONObject.parseObject(str1);
        if ("00".equals(json.getString("error_code"))) {
            JSONArray jsonArray = json.getJSONObject("data").getJSONArray("roles");
            if (!jsonArray.isEmpty()) {
                List<JSONObject> list = jsonArray.toJavaList(JSONObject.class);
                for (JSONObject j : list) {
                    if (setRoles.contains(j.getString("name"))) roleId.add(j.getIntValue("id"));
                }
            }
        }
        log.info("查询角色列表：返参：{}", str1);
        for (Integer id : roleId) {
            String str2 = eventRpc.getRolesDetail(InfrasConstant.KE_RPC_COOKIE, id);
            JSONObject json2 = JSONObject.parseObject(str2);
            if ("00".equals(json2.getString("error_code"))) {
                JSONArray jsonArray = json2.getJSONObject("data").getJSONArray("roles");
                if (!jsonArray.isEmpty()) {
                    List<JSONObject> list = jsonArray.toJavaList(JSONObject.class);
                    for (JSONObject j : list) {
                        touser.add(j.getString("account"));
                    }
                }
            }
            log.info("查询角色详情：返参：{}", str2);
        }
        //渠道配置
        Map<String, Object> impushChannel = new HashMap<>();//即时通消息渠道
        impushChannel.put("impushImportant", "yes"); //强制发送 yes:强制发送；no:正常
        impushChannel.put("impushContent", message);//即时通消息内容
        impushChannel.put("url", "测试跳转地址url");//待办消息跳转到具体页面地址
        map.put("impushChannel", impushChannel);
        //推送
        String mapStr = JSONObject.toJSONString(map);
        Object requestJson = JSON.parse(mapStr);
        log.info("移动OA消息推送，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson);
        String token = BusinessUtil.getToken();
        String str = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson);
        log.info("移动OA消息推送，出参：{}", str);
        JSONObject res = JSONObject.parseObject(str);
        return BaseResult.success(res);
    }

    /**
     * 测试OA发送邮件
     */
    @RequestMapping("/sendEmail")
    public BaseResult<JSONObject> test2(String userName, String message) {
        //构造参数
        log.info("移动OA发送邮件开始...");
        Map<String, Object> map = new HashMap<>();
        map.put("appId", "1125");//应用编号，行方开通的唯一编号
        map.put("subject", "邮件标题：测试");//邮件标题

        List<String> touser = new ArrayList<>();//用户名，qiaohesong,wangyongjun1
        touser.add(userName);
        map.put("touser", touser);

        Map<String, Object> emailMap = new HashMap<>();//邮件渠道
        emailMap.put("emailImportant", "yes");//强制发送 yes:强制发送；no:正常
        emailMap.put("emailContent", message);//邮件内容
        map.put("emailChannel", emailMap);

        String mapStr = JSONObject.toJSONString(map);
        Object requestJson = JSON.parse(mapStr);
        log.info("移动OA发送邮件，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson);
        String token = BusinessUtil.getToken();
        String str = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson);
        log.info("移动OA发送邮件，出参：{}", str);
        JSONObject res = JSONObject.parseObject(str);
        return BaseResult.success(res);
    }

    /**
     * 测试飞书发送代办消息
     */
    @RequestMapping("/sendFeishu")
    public BaseResult<JSONObject> sendFeishu(String userName, String msgtype, String message) {
        //构造参数
        log.info("飞书发送代办消息开始...");
        Map<String, Object> map = new HashMap<>();
        map.put("appId", "1125");//应用编号，行方开通的唯一编号

        List<String> touser = new ArrayList<>();//用户名，qiaohesong,wangyongjun1
        touser.add(userName);
        map.put("touser", touser);

        Map<String, Object> feishuMap = new HashMap<>();//飞书渠道
        feishuMap.put("urgent", true);//是否紧急
        feishuMap.put("msgtype", msgtype);//消息类型
        JSONObject content = new JSONObject();
        content.put("text", message);
        feishuMap.put("content", content.toJSONString());//消息内容
        System.out.println(content.toJSONString());
        feishuMap.put("important", true);//强制发送
        feishuMap.put("emailImportant", true);//强制发送 yes:强制发送；no:正常
        feishuMap.put("callbackUrl", "飞书机器人回调接口");//回调接口
        map.put("feishuChannel", feishuMap);

        String mapStr = JSONObject.toJSONString(map);
        Object requestJson = JSON.parse(mapStr);
        log.info("飞书发送代办消息，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson);
        String token = BusinessUtil.getToken();
        String str = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson);
        log.info("飞书发送代办消息，出参：{}", str);
        JSONObject res = JSONObject.parseObject(str);
        return BaseResult.success(res);
    }

}
