package com.enterprise.ai.messaging;

//import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AiConfig {

    @Value("${AI_API_KEY:missing}")
    private String apiKey;

    @Value("${AI_MODEL:gemini-3.6-flash}")
    private String modelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/") // Gemini's OpenAI-compatible URL
                .modelName(modelName)
                .build();
    }
}
