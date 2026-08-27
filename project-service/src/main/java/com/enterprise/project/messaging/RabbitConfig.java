package com.enterprise.project.messaging; // Use 'com.enterprise.ai.messaging' for ai-service

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "prototype.generation.mode", havingValue = "async", matchIfMissing = true)
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
