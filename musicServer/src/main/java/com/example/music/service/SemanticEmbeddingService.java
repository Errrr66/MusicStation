package com.example.music.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SemanticEmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<Double>> embeddingCache = new ConcurrentHashMap<>();

    @Value("${agent.rag.semantic.enabled:false}")
    private boolean semanticEnabled;

    @Value("${agent.rag.semantic.api-url:}")
    private String semanticApiUrl;

    @Value("${agent.rag.semantic.api-key:}")
    private String semanticApiKey;

    @Value("${agent.rag.semantic.model:text-embedding-v3}")
    private String semanticModel;

    @Value("${agent.rag.semantic.max-text-length:1000}")
    private int maxTextLength;

    public SemanticEmbeddingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return semanticEnabled
                && semanticApiUrl != null
                && !semanticApiUrl.trim().isEmpty()
                && semanticApiKey != null
                && !semanticApiKey.trim().isEmpty();
    }

    public double semanticSimilarity(String query, String text) {
        if (!isConfigured() || isBlank(query) || isBlank(text)) {
            return 0D;
        }

        try {
            List<Double> qVec = embedding(query);
            List<Double> tVec = embedding(text);
            return cosine(qVec, tVec);
        } catch (Exception ignored) {
            return 0D;
        }
    }

    private List<Double> embedding(String rawText) {
        String normalized = normalize(rawText);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        List<Double> cached = embeddingCache.get(normalized);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<Double> fetched = fetchEmbedding(normalized);
        if (!fetched.isEmpty()) {
            embeddingCache.put(normalized, fetched);
        }
        return fetched;
    }

    private List<Double> fetchEmbedding(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(semanticApiKey);

        String payload = buildEmbeddingPayload(text);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(semanticApiUrl, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return Collections.emptyList();
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return Collections.emptyList();
            }
            JsonNode embeddingNode = data.get(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                return Collections.emptyList();
            }

            List<Double> vector = new ArrayList<>(embeddingNode.size());
            for (JsonNode item : embeddingNode) {
                vector.add(item.asDouble());
            }
            return vector;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String buildEmbeddingPayload(String input) {
        try {
            Map<String, Object> body = Map.of(
                    "model", semanticModel,
                    "input", input
            );
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "{\"model\":\"" + semanticModel + "\",\"input\":\"" + input + "\"}";
        }
    }

    private String normalize(String text) {
        if (isBlank(text)) {
            return "";
        }
        String value = text.trim().replaceAll("\\s+", " ");
        if (value.length() <= maxTextLength) {
            return value;
        }
        return value.substring(0, maxTextLength);
    }

    private double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty() || a.size() != b.size()) {
            return 0D;
        }

        double dot = 0D;
        double aNorm = 0D;
        double bNorm = 0D;
        for (int i = 0; i < a.size(); i++) {
            double av = a.get(i);
            double bv = b.get(i);
            dot += av * bv;
            aNorm += av * av;
            bNorm += bv * bv;
        }
        if (aNorm == 0D || bNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(aNorm) * Math.sqrt(bNorm));
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}


