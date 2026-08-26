package com.enterprise.ai.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProjectEventConsumer {
    private final ChatLanguageModel model;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public ProjectEventConsumer(ChatLanguageModel model, RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper, @Value("${AI_API_KEY:}") String apiKey) {
        this.model = model;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @RabbitListener(queues = RabbitConfig.REQUEST_QUEUE)
    public void generate(String message) throws Exception {
        PrototypeRequest request = objectMapper.readValue(message, PrototypeRequest.class);
        publish(new PrototypeResult(request.projectId(), "GENERATING", null, null));
        try {
            if (apiKey.isBlank()) throw new IllegalStateException("AI_API_KEY is not configured");
            String response = model.generate(prompt(request));
            String cleaned = response.replace("```json", "").replace("```", "").trim();
            PrototypeSpecification spec = objectMapper.readValue(cleaned, PrototypeSpecification.class);
            validate(spec);
            publish(new PrototypeResult(request.projectId(), "COMPLETED", objectMapper.writeValueAsString(spec), null));
        } catch (Exception exception) {
            publish(new PrototypeResult(request.projectId(), "FAILED", null, safeMessage(exception)));
        }
    }

    private String prompt(PrototypeRequest request) {
        return """
            You are a product designer creating a concrete clickable application prototype specification.
            Base every choice on the supplied project description. Return ONLY valid JSON, without markdown.
            Use this exact top-level shape:
            {
              "projectName": "string",
              "overview": "specific 2-3 sentence overview",
              "keyFeatures": ["string"],
              "userRoles": [{"name":"string","description":"string"}],
              "navigationFlow": [{"from":"screen-id","action":"string","to":"screen-id"}],
              "screens": [{
                "id":"unique-kebab-case-id", "title":"string", "purpose":"string",
                "components":[{"type":"header|text|button|input|list|card|stat|table","label":"string","content":"sample content","action":"target-screen-id or empty"}]
              }],
              "recommendedTechStack": {
                "frontend": [{"name":"technology","reason":"one concise project-specific reason"}],
                "backend": [{"name":"technology","reason":"one concise project-specific reason"}],
                "database": [{"name":"technology","reason":"one concise project-specific reason"}],
                "aiIntegrations": [{"name":"technology or integration","reason":"one concise project-specific reason"}],
                "toolsDeployment": [{"name":"technology","reason":"one concise project-specific reason"}]
              }
            }
            Include 3-6 useful screens. Every non-empty component action must match a screen id.
            Recommend the technology stack from this project's name, description, generated features, user roles,
            workflow, scale, data, security, and integration needs. Do not default to a generic stack and do not
            copy technologies from unrelated examples. Return 2-4 focused choices per applicable category.
            Always include frontend, backend, database, and toolsDeployment. Include aiIntegrations only when the
            product genuinely needs AI capabilities or external services; otherwise return an empty array.

            Project name: %s
            Detailed description: %s
            """.formatted(request.projectName(), request.description());
    }

    private void validate(PrototypeSpecification spec) {
        if (spec == null || isEmpty(spec.keyFeatures()) || isEmpty(spec.userRoles())
                || isEmpty(spec.navigationFlow()) || isEmpty(spec.screens())
                || spec.recommendedTechStack() == null
                || isEmpty(spec.recommendedTechStack().frontend())
                || isEmpty(spec.recommendedTechStack().backend())
                || isEmpty(spec.recommendedTechStack().database())
                || isEmpty(spec.recommendedTechStack().toolsDeployment())) {
            throw new IllegalArgumentException("AI returned an incomplete prototype specification");
        }
        validateTechnologies(spec.recommendedTechStack().frontend());
        validateTechnologies(spec.recommendedTechStack().backend());
        validateTechnologies(spec.recommendedTechStack().database());
        validateTechnologies(spec.recommendedTechStack().toolsDeployment());
        if (spec.recommendedTechStack().aiIntegrations() != null) {
            validateTechnologies(spec.recommendedTechStack().aiIntegrations());
        }
    }

    private boolean isEmpty(java.util.List<?> values) {
        return values == null || values.isEmpty();
    }

    private void validateTechnologies(java.util.List<PrototypeSpecification.Technology> technologies) {
        boolean incomplete = technologies.stream().anyMatch(item -> item == null || item.name() == null
                || item.name().isBlank() || item.reason() == null || item.reason().isBlank());
        if (incomplete) throw new IllegalArgumentException("AI returned a technology without a reason");
    }

    private void publish(PrototypeResult result) throws Exception {
        rabbitTemplate.convertAndSend(RabbitConfig.RESULT_QUEUE, objectMapper.writeValueAsString(result));
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "Prototype generation failed";
        return message.length() > 900 ? message.substring(0, 900) : message;
    }
}
