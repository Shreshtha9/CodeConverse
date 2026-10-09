package com.shreshtha.codeconverse.backend.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Calls Jina's embeddings REST API directly.
 * Spring AI's OpenAI embedding client requires "usage.prompt_tokens" in the
 * response, which Jina does not send, so we bypass it.
 */
@Slf4j
public class JinaEmbeddingModel implements EmbeddingModel {

    private static final int BATCH_SIZE = 32;
    private static final int MAX_ATTEMPTS = 5;
    private static final int DIMENSIONS = 1024;

    private final RestClient client;
    private final String model;

    public JinaEmbeddingModel(String apiKey, String model) {
        this.model = model;
        this.client = RestClient.builder()
                .baseUrl("https://api.jina.ai")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        List<Embedding> result = new ArrayList<>();

        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            List<String> batch = texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()));
            for (float[] vector : embedBatch(batch)) {
                result.add(new Embedding(vector, result.size()));
            }
        }
        return new EmbeddingResponse(result);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @SuppressWarnings("unchecked")
    private List<float[]> embedBatch(List<String> batch) {
        Map<String, Object> body = Map.of(
                "model", model,
                "dimensions", DIMENSIONS,
                "input", batch);

        for (int attempt = 1; ; attempt++) {
            try {
                Map<String, Object> response = client.post()
                        .uri("/v1/embeddings")
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                float[][] ordered = new float[data.size()][];
                for (Map<String, Object> item : data) {
                    int index = ((Number) item.get("index")).intValue();
                    List<Number> values = (List<Number>) item.get("embedding");
                    float[] vector = new float[values.size()];
                    for (int i = 0; i < vector.length; i++) {
                        vector[i] = values.get(i).floatValue();
                    }
                    ordered[index] = vector;
                }
                return List.of(ordered);
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() != 429 || attempt >= MAX_ATTEMPTS) {
                    throw ex;
                }
                long waitMs = 20_000L * attempt;
                log.warn("Jina rate limit hit, retrying in {}s (attempt {}/{})",
                        waitMs / 1000, attempt, MAX_ATTEMPTS);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for Jina rate limit", ie);
                }
            }
        }
    }
}