package com.shreshtha.codeconverse.backend.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        Long githubId,
        String githubUsername,
        String displayName,
        String avatarUrl
) {
//    public UserResponse(UUID id, Long githubId, String githubUsername, String displayName, String avatarUrl) {
//        this.id = id;
//        this.githubId = githubId;
//        this.githubUsername = githubUsername;
//        this.displayName = displayName;
//        this.avatarUrl = avatarUrl;
//    }
}