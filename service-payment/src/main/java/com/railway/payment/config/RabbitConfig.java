package com.railway.payment.config;

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置（生产者侧）。
 * <ul>
 *   <li>声明 {@link MqConstant#ORDER_EXCHANGE} 主题交换机</li>
 *   <li>声明 {@link MqConstant#ORDER_PAID_QUEUE} 队列 + 绑定 RK {@link MqConstant#RK_ORDER_PAID}</li>
 *   <li>RabbitTemplate 用 Jackson JSON 序列化</li>
 * </ul>
 * <p><b>消费侧</b>在 service-order 也会声明同名队列（RabbitAdmin 幂等，重复声明 noop）。
 */
@Configuration
@EnableConfigurationProperties(PayProperties.class)
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
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        // 发送失败回调（demo 阶段只打日志，不重试）
        template.setMandatory(true);
        return template;
    }
}
