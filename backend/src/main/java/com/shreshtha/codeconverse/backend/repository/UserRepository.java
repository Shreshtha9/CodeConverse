package com.shreshtha.codeconverse.backend.repository;

import com.shreshtha.codeconverse.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGithubId(long githubId);
}
