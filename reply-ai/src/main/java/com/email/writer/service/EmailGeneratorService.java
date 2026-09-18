package com.email.writer.service;

import com.email.writer.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;
    private final String apiKey;

    public EmailGeneratorService(
            WebClient.Builder webClientBuilder,
            @Value("${gemini.api.url}") String baseUrl,
            @Value("${gemini.api.key}") String geminiApiKey) {

        this.apiKey = geminiApiKey;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public String generateEmailReply(EmailRequest emailRequest) {

        // Build prompt
        String prompt = buildPrompt(emailRequest);

        // Prepare JSON body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gemini-3.8-flash");
        requestBody.put("input", prompt);

        // Send request
        String response = webClient.post()
                .uri("/v1beta/interactions")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Extract response
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {

        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode root = mapper.readTree(response);

            return root.path("steps")
                    .get(1)
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("Generate a professional email reply for the following email:");

        if (emailRequest.getTone() != null &&
                !emailRequest.getTone().isEmpty()) {

            prompt.append(" Use a ")
                    .append(emailRequest.getTone())
                    .append(" tone.");
        }

        prompt.append(" Original Email: \n")
                .append(emailRequest.getEmailContent());

        return prompt.toString();
    }
}