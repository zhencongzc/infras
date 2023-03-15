package com.cmbc.infras.system.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cmbc.infras.constant.InfrasConstant;
import com.cmbc.infras.dto.Bank;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.dto.rpc.event.Event;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.mapper.DataConfigMapper;
import com.cmbc.infras.system.rpc.EventRpc;
import com.cmbc.infras.system.rpc.MobileOARpc;
import com.cmbc.infras.system.rpc.RpcUtil;
import com.cmbc.infras.system.service.ConfigService;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.system.util.BusinessUtil;
import com.cmbc.infras.system.util.TransferUtil;
import com.cmbc.infras.system.websocket.WebSocketServer;
import com.cmbc.infras.util.MQConfig;
import com.cmbc.infras.util.YmlConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * 告警MQ消费
 * queue: eventTopicQueue
 * exchange: storage_exchange
 * routing: rt.storage.eventtopic.admin
 */
@Slf4j(topic = "EventListener")
@Component
public class EventListener {

    private String sessionId;

    @Resource
    private FlowFormService flowFormService;
    @Resource
    private ConfigService configService;

    @Resource
    private DataConfigMapper dataConfigMapper;

    @Resource
    private MobileOARpc mobileOARpc;
    @Resource
    private EventRpc eventRpc;

    @Resource
    private WebSocketServer webSocketServer;

    @RabbitListener(queues = "${event-mq.queue}")
    public void onMessage(String message) throws InterruptedException {
        if (YmlConfig.getBoolValue("MQLogOpen")) log.info("EventListener.onMessage receive:{}", message);
        if (StringUtils.isBlank(message)) log.error("EventListener.onMessage[{}] message is blank!", MQConfig.queue);
        EventMessage eventMessage;
        try {
            eventMessage = JSON.parseObject(message, EventMessage.class);
        } catch (Exception e) {
            log.error("EventListener.onMessage[{}] parseObject error! message:{}", MQConfig.queue, message);
            return;
        }
        EventArgs args = eventMessage.getArgs();
        if (args == null) {
            log.error("EventListener.onMessage[{}] args is null! message:{}", MQConfig.queue, message);
            return;
        }
        Event event = args.getData();
        if (event == null) {
            log.error("EventListener.onMessage[{}] data is null! message:{}", MQConfig.queue, message);
            return;
        }
        AlarmEvent alarm = TransferUtil.eventToAlarm(event);
        //获取登录sessionId，以管理员身份后台登录
        if (StringUtils.isBlank(sessionId)) {
            //从redis获取token，没有执行后台登录，获取token存入redis并设置90min失效时间
            String token = DataRedisUtil.getStringFromRedis("login_ke_token");
            log.info("EventListener onMessage token:{}", token);
            if (token == null) {
                //防止缓存击穿，互斥锁设置成功获取token并设置，失败则等待2秒再获取token
                if (DataRedisUtil.addStringToRedisByExpireTime("login_ke_token_mutex", "1", 1000 * 60l) != null) {
                    token = RpcUtil.getToken();
                    DataRedisUtil.addStringToRedisByExpireTime("login_ke_token", token, 1000 * 60 * 90l);
                } else {
                    Thread.sleep(2 * 1000);
                    token = DataRedisUtil.getStringFromRedis("login_ke_token");
                }
            }
            sessionId = token;
            log.info("EventListener onMessage sessionId:{}", sessionId);
        }
        //可配告警等级，配置的等级才推送前台，默认使用admin告警等级配置
        List<Integer> levels = configService.getAlarmLevelShow();
        HashSet<String> ids = flowFormService.getAllBankIds(sessionId);
        String location = alarm.getEventLocation(); //project_root/0_931/0_969/0_1083/0_1183
        String[] arr = location.split("/");
        String rid = arr.length > 2 ? arr[2] : "-1";
        if (ids.contains(rid)) {
            Bank bank = flowFormService.getBankById(rid, sessionId);
            //设置联系人
            String contact = bank.getContact();
            if (StringUtils.isBlank(contact)) {
                alarm.setContact("-");
            } else {
                alarm.setContact(contact);
            }
            //设置bankId,bankName
            alarm.setBankId(bank.getBankId());
            alarm.setBankName(bank.getBankName());
        } else {
            //未查到银行id，可能是分行的灾备id，不推送该告警
            return;
        }
        //如果配置没空,没有配置,则默认是全部告警等级推送
        if (levels == null || levels.isEmpty()) {
            sendMsg(alarm);
        } else {
            if (levels.contains(alarm.getEventLevel())) sendMsg(alarm);
        }
    }

    private void sendMsg(AlarmEvent alarm) {
        try {
            //推送告警
            webSocketServer.send(alarm);
            //移动OA推送消息
            if (YmlConfig.loginTest != null && !"true".equals(YmlConfig.loginTest)) {
                //创建消息内容
                String content = alarm.getIsConfirm() == 0 ? "请及时处理！" : "已恢复！";
                content = alarm.getEventTimeShow() + alarm.getEventSource() + "发生\"" +
                        alarm.getContent() + "\"" + alarm.getEventLevelName() + "告警，" + content;

                //基础信息
                String eventLevel = String.valueOf(alarm.getEventLevel());
                String[] source = alarm.getEventSource().split("/");
                String area = source.length > 2 ? source[1] : "-1";

                //通过告警等级和所属分行查询需要发送代办消息的用户
                List<String> touser = dataConfigMapper.findUserNeedInformWithTwoCondition(area, "department", eventLevel, "alarmMessage");
                List<String> touserDefault1 = dataConfigMapper.findUserNeedInformDefault("中国民生银行", eventLevel, "alarmMessage");//总行权限的用户
                touser.addAll(touserDefault1);
                //发送飞书代办消息
                Object requestJson = BusinessUtil.createParamForFeishu(content, touser);
                log.info("i民生消息推送，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson);
                String token = BusinessUtil.getToken();
                String str = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson);
                log.info("i民生消息推送，出参：{}", str);

                //通过告警等级和所属分行查询需要发送邮件的用户
                List<String> touser2 = dataConfigMapper.findUserNeedInformWithTwoCondition(area, "department", eventLevel, "alarmEmail");
                List<String> touserDefault2 = dataConfigMapper.findUserNeedInformDefault("中国民生银行", eventLevel, "alarmEmail");//总行权限的用户
                touser2.addAll(touserDefault2);
                //发送邮件
                Object requestJson2 = BusinessUtil.createParamForSendEmail(content, touser2);
                log.info("移动OA发送邮件，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson2);
                String str2 = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson2);
                log.info("移动OA发送邮件，出参：{}", str2);

                //兼容老移动OA代办消息
                List<String> touserOld = new ArrayList<>();//用户名
                //查询告警所属分行，查询KE角色：xx分行-基础设施管理员、xx分行-中心机房管理员、分行机房管理员，分别添加对应人员
                String bankName = alarm.getBankName();
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
                for (Integer id : roleId) {
                    String str3 = eventRpc.getRolesDetail(InfrasConstant.KE_RPC_COOKIE, id);
                    JSONObject json2 = JSONObject.parseObject(str3);
                    if ("00".equals(json2.getString("error_code"))) {
                        JSONArray jsonArray = json2.getJSONObject("data").getJSONArray("roles");
                        if (!jsonArray.isEmpty()) {
                            List<JSONObject> list = jsonArray.toJavaList(JSONObject.class);
                            for (JSONObject j : list) {
                                touserOld.add(j.getString("account"));
                            }
                        }
                    }
                }
                Object requestJson3 = BusinessUtil.createParamForSendMessage(content, touserOld);
                log.info("移动OA发送消息，请求url：{}，入参：{}", "/sqs/api/queue/restSendUnifyMessage", requestJson3);
                String str4 = mobileOARpc.restSendUnifyMessageWithToken(token, requestJson3);
                log.info("移动OA发送消息，出参：{}", str4);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("EventListener.onMessage[{}] WebSocketServer.send error! error_message:{}", MQConfig.queue, e.getMessage());
        }
    }


}

class EventMessage {
    private EventArgs args;

    public EventArgs getArgs() {
        return args;
    }

    public void setArgs(EventArgs args) {
        this.args = args;
    }
}

@Data
class EventArgs {
    private Event data;
}