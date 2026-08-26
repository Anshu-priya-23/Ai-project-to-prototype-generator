package com.enterprise.project.controller;

import com.enterprise.project.messaging.ProjectEventProducer;
import com.enterprise.project.entity.Project;
import com.enterprise.project.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.enterprise.project.entity.PrototypeStatus;
import com.enterprise.project.messaging.PrototypeRequest;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final ProjectEventProducer eventProducer;

    public ProjectController(ProjectRepository projectRepository, ProjectEventProducer eventProducer) {
        this.projectRepository = projectRepository;
        this.eventProducer = eventProducer;
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
        project.setPrototypeStatus(PrototypeStatus.QUEUED);
        project.setPrototypeError(null);
        project = projectRepository.save(project);
        eventProducer.requestPrototype(new PrototypeRequest(project.getId(), project.getName(), project.getDescription()));
        return ResponseEntity.accepted().body(project);
    }
}
