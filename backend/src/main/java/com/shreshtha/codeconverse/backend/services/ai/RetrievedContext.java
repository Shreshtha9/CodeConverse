package com.shreshtha.codeconverse.backend.services.ai;

import java.util.List;

import com.shreshtha.codeconverse.backend.dto.CitationDto;

public record RetrievedContext(
        List<CitationDto> citations,
        String contextText) {
}
