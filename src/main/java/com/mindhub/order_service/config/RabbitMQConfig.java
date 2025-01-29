package com.mindhub.order_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // Define a RabbitMQ queue for "orderCreatedEvent"
    @Bean
    public Queue queueOrderCreatedEvent() {
        return new Queue("orderCreatedEvent", false);
    }
    // Define a RabbitMQ queue for testing purposes
    @Bean
    public Queue queue() {
        return new Queue("testingQueue1", false);
    }
    @Bean
    public Queue queue2() {
        return new Queue("testingQueue2", false);
    }
    // Define a RabbitMQ topic exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("testingExchange");
    }
    // Bind the "orderCreatedEvent" queue to the exchange with a routing key
    @Bean
    public Binding bindingQueueOrderCreatedEvent(Queue queueOrderCreatedEvent, TopicExchange exchange) {
        return BindingBuilder.bind(queueOrderCreatedEvent).to(exchange).with("routingOrderCreatedEvent.key");
    }

    @Bean
    public Binding bindingQueue(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("routing.key");
    }

    @Bean
    public Binding bindingQueue2(Queue queue2, TopicExchange exchange) {
        return BindingBuilder.bind(queue2).to(exchange).with("routing.key2");
    }
    // Configure a JSON message converter for RabbitMQ messages
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    // Configure RabbitTemplate with the JSON message converter
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter); // Configura JSON como convertidor
        return rabbitTemplate;
    }

}
