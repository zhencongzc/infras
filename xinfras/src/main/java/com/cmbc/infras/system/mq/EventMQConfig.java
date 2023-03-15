package com.cmbc.infras.system.mq;

import com.cmbc.infras.util.MQConfig;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventMQConfig {

    //private static final String key = "rt.storage.eventtopic.admin";

    @Bean
    public Queue eventTopicQueue() {
        return new Queue(MQConfig.queue);
    }

    @Bean
    public TopicExchange storageExchange() {
        return new TopicExchange(MQConfig.exchange);
    }

    @Bean
    public Binding bindingEventTopicExchange() {
        return BindingBuilder.bind(eventTopicQueue()).to(storageExchange()).with(MQConfig.routing);
    }

}
