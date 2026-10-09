package com.shreshtha.codeconverse.backend.dto;



import com.shreshtha.codeconverse.backend.entity.IndexStatus;

import java.time.Instant;
import java.util.UUID;



public record IndexStatusResponse(
        UUID repositoryId,
        IndexStatus indexStatus,
        int filesTotal,
        int filesProcessed,
        int chunkCount,
        Instant indexedAt,
        String errorMessage) {
}
