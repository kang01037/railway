package com.railway.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railway.common.constant.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置（消费侧）。
 *
 * <h3>关键：延迟关单</h3>
 * <pre>
 *  send ──▶ orderDelayExchange (Direct) ──order.delay──▶ orderDelayQueue
 *                                                        │ TTL=15min
 *                                                        │ (无消费者)
 *                                                        ▼ DLX
 *                                              orderExchange (Topic) ──order.cancel──▶ orderCancelQueue
 *                                                                                              │
 *                                                                                              ▼
 *                                                                       OrderDelayCancelConsumer
 * </pre>
 */
@Configuration
public class RabbitConfig {

    // ============= 主题交换机：order.exchange =============
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(MqConstant.ORDER_EXCHANGE, true, false);
    }

    // 削峰填谷：订单创建队列（prefetch=100 控制并发）
    @Bean
    public Queue orderCreateQueue() {
        return QueueBuilder.durable(MqConstant.ORDER_CREATE_QUEUE)
                .withArgument("x-message-ttl", 60_000)  // 消息 60 秒过期
                .build();
    }

    @Bean
    public Binding orderCreateBinding(Queue orderCreateQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCreateQueue).to(orderExchange).with(MqConstant.RK_ORDER_CREATE);
    }

    // 收 payment.callback 发来的 order.paid
    @Bean
    public Queue orderPaidQueue() {
        return new Queue(MqConstant.ORDER_PAID_QUEUE, true);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with(MqConstant.RK_ORDER_PAID);
    }

    // 收 延迟队列 DLX 推来的 order.cancel
    @Bean
    public Queue orderCancelQueue() {
        return new Queue(MqConstant.ORDER_CANCEL_QUEUE, true);
    }

    @Bean
    public Binding orderCancelBinding(Queue orderCancelQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCancelQueue).to(orderExchange).with(MqConstant.RK_ORDER_CANCEL);
    }

    // ============= 延迟交换机：order.delay.exchange =============
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(MqConstant.ORDER_DELAY_EXCHANGE, true, false);
    }

    // 延迟队列（TTL=15min，DLX=orderExchange, DLRK=order.cancel）
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(MqConstant.ORDER_DELAY_QUEUE)
                .withArgument("x-message-ttl", 900_000)                                       // 15 min
                .withArgument("x-dead-letter-exchange", MqConstant.ORDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstant.RK_ORDER_CANCEL)
                .build();
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderDelayExchange).with(MqConstant.RK_ORDER_DELAY);
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
