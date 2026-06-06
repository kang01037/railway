package com.railway.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.common.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务的 RabbitMQ 声明。
 *
 * <p>消费侧需要：把 {@code order.paid.queue} / {@code order.cancel.queue} 显式声明出来
 * （RabbitAdmin 在 RabbitTemplate 启动时建队列）。
 * <p>交换机 {@code orderExchange} 由 service-payment 首发，service-order 消费侧已声明；
 * 此处再声明一次 → RabbitAdmin 幂等 noop。
 */
@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(MqConstant.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue(MqConstant.ORDER_PAID_QUEUE, true);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with(MqConstant.RK_ORDER_PAID);
    }

    @Bean
    public Queue orderCancelQueue() {
        return new Queue(MqConstant.ORDER_CANCEL_QUEUE, true);
    }

    @Bean
    public Binding orderCancelBinding(Queue orderCancelQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCancelQueue).to(orderExchange).with(MqConstant.RK_ORDER_CANCEL);
    }

    // ============= 序列化 =============
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }
}
