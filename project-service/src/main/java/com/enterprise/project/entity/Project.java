package com.enterprise.project.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    @Column(length = 1000)
    private String description;
    
    private String ownerEmail;

    @Enumerated(EnumType.STRING)
    private PrototypeStatus prototypeStatus = PrototypeStatus.NOT_STARTED;

    @Column(columnDefinition = "TEXT")
    private String prototypeSpec;

    @Column(length = 1000)
    private String prototypeError;

    public Project() {}

    public Project(String name, String description, String ownerEmail) {
        this.name = name;
        this.description = description;
        this.ownerEmail = ownerEmail;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public PrototypeStatus getPrototypeStatus() { return prototypeStatus; }
    public void setPrototypeStatus(PrototypeStatus prototypeStatus) { this.prototypeStatus = prototypeStatus; }

    public String getPrototypeSpec() { return prototypeSpec; }
    public void setPrototypeSpec(String prototypeSpec) { this.prototypeSpec = prototypeSpec; }

    public String getPrototypeError() { return prototypeError; }
    public void setPrototypeError(String prototypeError) { this.prototypeError = prototypeError; }
}
