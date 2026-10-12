package com.computedeliver.orders.events;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "orders.events";
    public static final String ORDER_INTAKEN_KEY = "order.intaken";
    /** Bound queue so the published events can be inspected until real consumers exist. */
    public static final String VERIFICATION_QUEUE = "orders.order-intaken.verification";

    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue orderIntakenVerificationQueue() {
        return QueueBuilder.durable(VERIFICATION_QUEUE).build();
    }

    @Bean
    Binding orderIntakenBinding(Queue orderIntakenVerificationQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderIntakenVerificationQueue).to(ordersExchange).with(ORDER_INTAKEN_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
