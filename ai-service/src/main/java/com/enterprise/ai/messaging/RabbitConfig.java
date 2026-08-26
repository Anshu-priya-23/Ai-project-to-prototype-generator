package com.enterprise.ai.messaging; // Use 'com.enterprise.ai.messaging' for ai-service

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String REQUEST_QUEUE = "prototype-generation-requests";
    public static final String RESULT_QUEUE = "prototype-generation-results";

    @Bean
    public Queue prototypeRequestQueue() {
        return new Queue(REQUEST_QUEUE, true);
    }

    @Bean
    public Queue prototypeResultQueue() {
        return new Queue(RESULT_QUEUE, true);
    }
}
