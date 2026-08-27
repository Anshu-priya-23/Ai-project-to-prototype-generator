package com.enterprise.project.controller;

import com.enterprise.project.messaging.ProjectEventProducer;
import com.enterprise.project.entity.Project;
import com.enterprise.project.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import com.enterprise.project.entity.PrototypeStatus;
import com.enterprise.project.messaging.PrototypeRequest;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final Optional<ProjectEventProducer> eventProducer;
    private final RestClient aiClient;
    private final String generationMode;

    public ProjectController(ProjectRepository projectRepository, Optional<ProjectEventProducer> eventProducer,
                             RestClient.Builder restClientBuilder,
                             @Value("${AI_SERVICE_HOST:ai-service}") String aiServiceHost,
                             @Value("${AI_SERVICE_PORT:8083}") int aiServicePort,
                             @Value("${prototype.generation.mode:async}") String generationMode) {
        this.projectRepository = projectRepository;
        this.eventProducer = eventProducer;
        this.aiClient = restClientBuilder.baseUrl("http://" + aiServiceHost + ":" + aiServicePort).build();
        this.generationMode = generationMode;
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(@RequestParam String ownerEmail) {
        List<Project> projects = projectRepository.findByOwnerEmailOrderByIdDesc(ownerEmail);
        return ResponseEntity.ok(projects);
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project projectRequest) {
        projectRequest.setPrototypeStatus(PrototypeStatus.NOT_STARTED);
        projectRequest.setPrototypeSpec(null);
        projectRequest.setPrototypeError(null);
        Project savedProject = projectRepository.save(projectRequest);
        return ResponseEntity.ok(savedProject);
    }

    @PostMapping("/{id}/prototype")
    public ResponseEntity<Project> generatePrototype(@PathVariable Long id) {
        Project project = projectRepository.findById(id).orElseThrow();
        project.setPrototypeStatus("sync".equalsIgnoreCase(generationMode)
                ? PrototypeStatus.GENERATING : PrototypeStatus.QUEUED);
        project.setPrototypeError(null);
        project = projectRepository.save(project);
        PrototypeRequest request = new PrototypeRequest(project.getId(), project.getName(), project.getDescription());
        if (!"sync".equalsIgnoreCase(generationMode)) {
            eventProducer.orElseThrow(() -> new IllegalStateException("RabbitMQ generation is not configured"))
                    .requestPrototype(request);
            return ResponseEntity.accepted().body(project);
        }

        try {
            com.enterprise.project.messaging.PrototypeResult result = aiClient.post()
                    .uri("/api/v1/ai/prototype")
                    .body(request)
                    .retrieve()
                    .body(com.enterprise.project.messaging.PrototypeResult.class);
            if (result == null) throw new IllegalStateException("AI service returned no result");
            project.setPrototypeStatus(PrototypeStatus.valueOf(result.status()));
            project.setPrototypeSpec(result.prototypeSpec());
            project.setPrototypeError(result.error());
        } catch (Exception exception) {
            project.setPrototypeStatus(PrototypeStatus.FAILED);
            String message = exception.getMessage();
            project.setPrototypeError(message == null || message.isBlank()
                    ? "AI service request failed" : message.substring(0, Math.min(message.length(), 900)));
        }
        return ResponseEntity.ok(projectRepository.save(project));
    }
}
