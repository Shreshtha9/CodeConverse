package com.shreshtha.codeconverse.backend.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JinaEmbeddingConfig {

    @Bean
    @Primary
    EmbeddingModel jinaEmbeddingModel(
            @Value("${jina.api-key}") String apiKey,
            @Value("${jina.model:jina-embeddings-v3}") String model) {
        return new JinaEmbeddingModel(apiKey, model);
    }
}