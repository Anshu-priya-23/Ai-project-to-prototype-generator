package com.enterprise.project.messaging;

import com.enterprise.project.entity.Project;
import com.enterprise.project.entity.PrototypeStatus;
import com.enterprise.project.repository.ProjectRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "prototype.generation.mode", havingValue = "async", matchIfMissing = true)
public class PrototypeResultConsumer {
    private final ProjectRepository repository;
    private final ObjectMapper objectMapper;

    public PrototypeResultConsumer(ProjectRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.RESULT_QUEUE)
    public void handle(String message) throws Exception {
        PrototypeResult result = objectMapper.readValue(message, PrototypeResult.class);
        Project project = repository.findById(result.projectId()).orElse(null);
        if (project == null) return;
        project.setPrototypeStatus(PrototypeStatus.valueOf(result.status()));
        project.setPrototypeSpec(result.prototypeSpec());
        project.setPrototypeError(result.error());
        repository.save(project);
    }
}
