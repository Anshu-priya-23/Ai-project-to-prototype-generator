package com.enterprise.ai.messaging;

import java.util.List;

public record PrototypeSpecification(
        String projectName,
        String overview,
        List<String> keyFeatures,
        List<UserRole> userRoles,
        List<NavigationStep> navigationFlow,
        List<Screen> screens,
        RecommendedTechStack recommendedTechStack) {

    public record UserRole(String name, String description) {}
    public record NavigationStep(String from, String action, String to) {}
    public record Screen(String id, String title, String purpose, List<Component> components) {}
    public record Component(String type, String label, String content, String action) {}
    public record Technology(String name, String reason) {}
    public record RecommendedTechStack(
            List<Technology> frontend,
            List<Technology> backend,
            List<Technology> database,
            List<Technology> aiIntegrations,
            List<Technology> toolsDeployment) {}
}
