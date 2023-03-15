package com.cmbc.infras.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MQConfig {

    //队列名
    public static String queue;
    //exchange名称
    public static String exchange;
    //路由KEY
    public static String routing;

    @Value("${event-mq.queue}")
    public void setQueue(String queueVal) {
        queue = queueVal;
    }

    @Value("${event-mq.exchange}")
    public void setExchange(String exchangeVal) {
        exchange = exchangeVal;
    }

    @Value("${event-mq.routing}")
    public void setRouting(String routingVal) {
        routing = routingVal;
    }
}
