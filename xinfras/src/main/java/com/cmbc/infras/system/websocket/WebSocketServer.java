package com.cmbc.infras.system.websocket;

import com.alibaba.fastjson.JSON;
import com.cmbc.infras.dto.BaseResult;
import com.cmbc.infras.dto.rpc.event.AlarmEvent;
import com.cmbc.infras.redis.DataRedisUtil;
import com.cmbc.infras.system.rpc.RpcUtil;
import com.cmbc.infras.system.service.FlowFormService;
import com.cmbc.infras.system.util.BusinessUtil;
import com.cmbc.infras.system.util.SpringCtxUtils;
import com.cmbc.infras.system.util.WSDecodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警页面-告警消息推送
 * 例子: ws://127.0.0.1:8085/infras/socket/admin:0:1658215810786
 * code成份: 用户账号:界面:统一认证code(也可以是token)
 * 页面: 总行主,总行运维,分行主,分行运维;
 * head-main,head-ops,sub-main,sub-ops
 */
@Slf4j
@Service
@ServerEndpoint("/socket/{code}")
public class WebSocketServer {

    private static ConcurrentHashMap<String, WebSocketServer> socketMap = new ConcurrentHashMap<>();

    private static int online = 0;

    private Session session;

    /**
     * "admin:head-main:ILJIEK"
     * 用户账号:界面:统一认证code(也可以是token)截取
     */
    private String code;
    /**
     * 从code中取account
     * 查询account对应bankId
     * 通过bankId过滤告警
     */
    private String resourceId;
    private List<String> bankIds;
    private String sessionId;

    @OnOpen
    public void onOpen(Session session, @PathParam("code") String code) throws InterruptedException {
        this.session = session;
        this.code = code;
        FlowFormService flowFormService = SpringCtxUtils.getBean(FlowFormService.class);
        if (StringUtils.isBlank(sessionId)) {
            //从redis获取token，没有执行后台登录，获取token存入redis并设置90min失效时间
            String token = DataRedisUtil.getStringFromRedis("login_ke_token");
            log.info("WebSocket 从redis获取login_ke_token:{}", token);
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
            log.info("WebSocket onOpen code:{}, sessionId:{}", code, sessionId);
        }
        BaseResult<String> idResource = WSDecodeUtil.getBankResourceId(code);
        if (idResource.isSuccess()) {
            this.resourceId = idResource.getData();
            bankIds = flowFormService.getCacheSubBankIds(this.resourceId, sessionId);
            bankIds.add(resourceId);
        } else {
            //code错误或者账号没有绑定银行
            log.error(idResource.getMessage());
            return;
        }
        if (socketMap.containsKey(code)) {
            socketMap.remove(code);
            socketMap.put(code, this);
        } else {
            socketMap.put(code, this);
            WebSocketServer.online++;
        }
        log.info("code:" + code + ",上线,当前在线数:" + WebSocketServer.online);
    }

    @OnClose
    public void onClose() {
        if (socketMap.containsKey(code)) {
            socketMap.remove(code);
            WebSocketServer.online--;
        }
        log.info("code:" + code + "下线,当前在线数:" + WebSocketServer.online);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("code" + code + "出错!信息:" + error.getMessage());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到用户:" + code + "信息:" + message);
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public void send(AlarmEvent alarm) throws IOException {
        if (alarm == null) {
            return;
        }
        for (String code : socketMap.keySet()) {
            WebSocketServer server = socketMap.get(code);
            if (BusinessUtil.alarmMatchBankId(alarm, server.bankIds)) {
                String msg = JSON.toJSONString(alarm);
                socketMap.get(code).sendMessage(msg);
            }
        }
    }

}

