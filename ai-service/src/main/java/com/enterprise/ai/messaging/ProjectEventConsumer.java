package com.enterprise.ai.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.enterprise.ai.service.PrototypeGenerator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "prototype.generation.mode", havingValue = "async", matchIfMissing = true)
public class ProjectEventConsumer {
    private final PrototypeGenerator generator;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public ProjectEventConsumer(PrototypeGenerator generator, RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper) {
        this.generator = generator;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.REQUEST_QUEUE)
    public void generate(String message) throws Exception {
        PrototypeRequest request = objectMapper.readValue(message, PrototypeRequest.class);
        publish(new PrototypeResult(request.projectId(), "GENERATING", null, null));
        publish(generator.generate(request));
    }

    private void publish(PrototypeResult result) throws Exception {
        rabbitTemplate.convertAndSend(RabbitConfig.RESULT_QUEUE, objectMapper.writeValueAsString(result));
    }

}
