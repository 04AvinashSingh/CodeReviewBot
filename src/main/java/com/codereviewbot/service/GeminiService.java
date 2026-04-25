package com.codereviewbot.service;

import com.codereviewbot.dto.CommentDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GeminiService {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are an expert code reviewer. Review this code diff and return \
            a JSON array of review comments. Each comment must have: \
            file_path (string), line_number (integer), severity (INFO/WARNING/ERROR), \
            comment (string, max 200 chars). Focus on: bugs, security issues, \
            performance problems, and bad practices. Return ONLY valid JSON, \
            no markdown. If the code looks fine, return an empty array [].
            """;

    // Pattern to extract JSON array from potentially wrapped response
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[\\s*\\{.*}\\s*]", Pattern.DOTALL);

    public GeminiService(WebClient geminiWebClient,
                         @Qualifier("geminiApiKey") String apiKey,
                         @Qualifier("geminiModel") String model,
                         ObjectMapper objectMapper) {
        this.webClient = geminiWebClient;
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the diff to Gemini and returns parsed review comments.
     *
     * @param filename the file being reviewed
     * @param patch    the unified diff / patch text
     * @return list of review comments, empty if no issues found
     */
    public GeminiResult reviewDiff(String filename, String patch) {
        String userPrompt = "File: " + filename + "\n\nDiff:\n```\n" + patch + "\n```";

        Map<String, Object> requestBody = buildRequestBody(userPrompt);

        try {
            String responseJson = webClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return parseResponse(responseJson);

        } catch (Exception e) {
            log.error("Gemini API call failed for file {}: {}", filename, e.getMessage(), e);
            return new GeminiResult(Collections.emptyList(), 0);
        }
    }

    private Map<String, Object> buildRequestBody(String userPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();

        // System instruction
        Map<String, Object> systemInstruction = new LinkedHashMap<>();
        systemInstruction.put("parts", List.of(Map.of("text", SYSTEM_PROMPT)));
        body.put("system_instruction", systemInstruction);

        // User content
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("parts", List.of(Map.of("text", userPrompt)));
        body.put("contents", List.of(content));

        // Generation config
        Map<String, Object> genConfig = new LinkedHashMap<>();
        genConfig.put("temperature", 0.2);
        genConfig.put("maxOutputTokens", 2048);
        genConfig.put("responseMimeType", "application/json");
        body.put("generationConfig", genConfig);

        return body;
    }

    GeminiResult parseResponse(String responseJson) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseJson, new TypeReference<>() {});

            // Extract token usage
            int tokensUsed = extractTokenUsage(response);

            // Extract text content from response
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.warn("Gemini returned no candidates");
                return new GeminiResult(Collections.emptyList(), tokensUsed);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            // Parse the JSON array from the text
            List<CommentDTO> comments = parseComments(text);

            log.info("Gemini returned {} comments, used {} tokens", comments.size(), tokensUsed);
            return new GeminiResult(comments, tokensUsed);

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage(), e);
            return new GeminiResult(Collections.emptyList(), 0);
        }
    }

    private List<CommentDTO> parseComments(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        String jsonText = text.trim();

        // Try direct parse first
        try {
            return objectMapper.readValue(jsonText, new TypeReference<List<CommentDTO>>() {});
        } catch (Exception e) {
            // Try to extract JSON array from potentially wrapped text (markdown etc.)
            Matcher matcher = JSON_ARRAY_PATTERN.matcher(jsonText);
            if (matcher.find()) {
                try {
                    return objectMapper.readValue(matcher.group(), new TypeReference<List<CommentDTO>>() {});
                } catch (Exception ex) {
                    log.warn("Failed to parse extracted JSON array: {}", ex.getMessage());
                }
            }

            // Check for empty array
            if (jsonText.equals("[]") || jsonText.contains("[]")) {
                return Collections.emptyList();
            }

            log.warn("Could not parse Gemini output as JSON: {}", jsonText.substring(0, Math.min(200, jsonText.length())));
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private int extractTokenUsage(Map<String, Object> response) {
        try {
            Map<String, Object> usageMetadata = (Map<String, Object>) response.get("usageMetadata");
            if (usageMetadata != null) {
                Number totalTokens = (Number) usageMetadata.get("totalTokenCount");
                return totalTokens != null ? totalTokens.intValue() : 0;
            }
        } catch (Exception e) {
            log.debug("Could not extract token usage: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Result wrapper containing both comments and token usage.
     */
    public record GeminiResult(List<CommentDTO> comments, int tokensUsed) {}
}
